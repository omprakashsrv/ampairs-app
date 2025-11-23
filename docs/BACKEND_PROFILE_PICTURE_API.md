# Backend API Requirements: Profile Picture Feature

This document describes the backend API changes required to support the profile picture feature in the Ampairs mobile application.

## Overview

The mobile app now supports profile picture upload and display. The backend needs to implement the following:

1. Add profile picture fields to the User entity
2. Create a multipart upload endpoint
3. Handle image storage (S3 or similar)
4. Serve profile pictures through API endpoints (NOT direct S3 URLs)

## Important: Image Serving Architecture

The mobile client does **NOT** use direct S3/storage URLs. Instead, the backend serves images through API endpoints:

```
# Current user's profile picture
GET /user/v1/picture

# Current user's thumbnail
GET /user/v1/picture/thumbnail

# Any user's profile picture by user ID
GET /user/v1/{userId}/picture

# Any user's thumbnail by user ID
GET /user/v1/{userId}/picture/thumbnail
```

The `profile_picture_url` field in the user response contains the internal storage key (e.g., `profile-pictures/UID.../profile_xxx.jpg`) - this is for backend reference only and should NOT be used by the client directly.

---

## 1. Database Schema Changes

### User Entity Updates

Add the following fields to the `User` entity:

```kotlin
@Entity
data class User(
    // ... existing fields ...

    @Column(name = "profile_picture_url")
    var profilePictureUrl: String? = null,

    @Column(name = "profile_picture_thumbnail_url")
    var profilePictureThumbnailUrl: String? = null,

    @Column(name = "profile_picture_updated_at")
    var profilePictureUpdatedAt: Instant? = null
)
```

### Migration Script

```sql
ALTER TABLE users ADD COLUMN profile_picture_url VARCHAR(500);
ALTER TABLE users ADD COLUMN profile_picture_thumbnail_url VARCHAR(500);
ALTER TABLE users ADD COLUMN profile_picture_updated_at TIMESTAMP;
```

---

## 2. API Endpoints

### 2.1 Upload Profile Picture (NEW)

**Endpoint**: `POST /user/v1/upload-picture`

**Content-Type**: `multipart/form-data`

**Request**:
- `file` (required): Image file (JPEG, PNG, or WebP)
- Max file size: 5MB
- Recommended dimensions: 512x512 pixels (will be resized if larger)

**Response** (200 OK):
```json
{
  "data": {
    "id": "user-123",
    "first_name": "John",
    "last_name": "Doe",
    "user_name": "johndoe",
    "country_code": 91,
    "phone": "9876543210",
    "profile_picture_url": "https://storage.ampairs.in/users/user-123/profile.jpg",
    "profile_picture_thumbnail_url": "https://storage.ampairs.in/users/user-123/profile_thumb.jpg"
  },
  "error": null
}
```

**Error Responses**:
- `400 Bad Request`: Invalid file type or size
- `401 Unauthorized`: Invalid/expired token
- `500 Internal Server Error`: Storage failure

### 2.2 Get User (UPDATED)

**Endpoint**: `GET /user/v1`

**Response** (200 OK):
```json
{
  "data": {
    "id": "user-123",
    "first_name": "John",
    "last_name": "Doe",
    "user_name": "johndoe",
    "country_code": 91,
    "phone": "9876543210",
    "profile_picture_url": "https://storage.ampairs.in/users/user-123/profile.jpg",
    "profile_picture_thumbnail_url": "https://storage.ampairs.in/users/user-123/profile_thumb.jpg"
  },
  "error": null
}
```

### 2.3 Update User (NO CHANGES)

The existing `POST /user/v1/update` endpoint remains unchanged. Profile picture is uploaded separately.

---

## 3. Implementation Guide

### 3.1 Controller

```kotlin
@RestController
@RequestMapping("/user/v1")
class UserController(
    private val userService: UserService,
    private val profilePictureService: ProfilePictureService
) {

    @PostMapping("/upload-picture", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadProfilePicture(
        @RequestPart("file") file: MultipartFile,
        @AuthenticationPrincipal user: UserPrincipal
    ): ResponseEntity<ApiResponse<UserDto>> {
        // Validate file
        validateProfilePicture(file)

        // Upload and get URLs
        val pictureUrls = profilePictureService.uploadProfilePicture(user.id, file)

        // Update user and return
        val updatedUser = userService.updateProfilePicture(user.id, pictureUrls)
        return ResponseEntity.ok(ApiResponse.success(updatedUser.toDto()))
    }

    private fun validateProfilePicture(file: MultipartFile) {
        val maxSize = 5 * 1024 * 1024L // 5MB
        val allowedTypes = listOf("image/jpeg", "image/png", "image/webp")

        if (file.size > maxSize) {
            throw BadRequestException("File size exceeds 5MB limit")
        }

        if (file.contentType !in allowedTypes) {
            throw BadRequestException("Invalid file type. Allowed: JPEG, PNG, WebP")
        }
    }
}
```

### 3.2 Profile Picture Service

```kotlin
@Service
class ProfilePictureService(
    private val storageService: StorageService
) {

    data class PictureUrls(
        val fullUrl: String,
        val thumbnailUrl: String
    )

    fun uploadProfilePicture(userId: String, file: MultipartFile): PictureUrls {
        // Generate unique filename
        val extension = file.originalFilename?.substringAfterLast(".") ?: "jpg"
        val filename = "profile_${System.currentTimeMillis()}.$extension"
        val thumbnailFilename = "profile_thumb_${System.currentTimeMillis()}.$extension"

        // Create thumbnail (256x256)
        val thumbnailBytes = createThumbnail(file.bytes, 256)

        // Resize original if larger than 512x512
        val resizedBytes = resizeIfNeeded(file.bytes, 512)

        // Upload to storage
        val basePath = "users/$userId"
        val fullUrl = storageService.upload("$basePath/$filename", resizedBytes, file.contentType!!)
        val thumbnailUrl = storageService.upload("$basePath/$thumbnailFilename", thumbnailBytes, file.contentType!!)

        // Delete old pictures if exist
        deleteOldProfilePictures(userId)

        return PictureUrls(fullUrl, thumbnailUrl)
    }

    private fun createThumbnail(imageBytes: ByteArray, size: Int): ByteArray {
        // Use Java ImageIO or a library like Thumbnailator
        val inputImage = ImageIO.read(ByteArrayInputStream(imageBytes))
        val thumbnail = Thumbnails.of(inputImage)
            .size(size, size)
            .crop(Positions.CENTER)
            .outputFormat("jpg")
            .asBufferedImage()

        val outputStream = ByteArrayOutputStream()
        ImageIO.write(thumbnail, "jpg", outputStream)
        return outputStream.toByteArray()
    }

    private fun resizeIfNeeded(imageBytes: ByteArray, maxSize: Int): ByteArray {
        val inputImage = ImageIO.read(ByteArrayInputStream(imageBytes))

        if (inputImage.width <= maxSize && inputImage.height <= maxSize) {
            return imageBytes
        }

        val resized = Thumbnails.of(inputImage)
            .size(maxSize, maxSize)
            .keepAspectRatio(true)
            .asBufferedImage()

        val outputStream = ByteArrayOutputStream()
        ImageIO.write(resized, "jpg", outputStream)
        return outputStream.toByteArray()
    }
}
```

### 3.3 Storage Service (Example: AWS S3)

```kotlin
@Service
class S3StorageService(
    private val s3Client: AmazonS3,
    @Value("\${aws.s3.bucket}") private val bucketName: String,
    @Value("\${aws.s3.base-url}") private val baseUrl: String
) : StorageService {

    override fun upload(path: String, data: ByteArray, contentType: String): String {
        val metadata = ObjectMetadata().apply {
            this.contentType = contentType
            this.contentLength = data.size.toLong()
            // Cache for 1 year (profile pictures are versioned by filename)
            this.cacheControl = "max-age=31536000"
        }

        s3Client.putObject(
            PutObjectRequest(bucketName, path, ByteArrayInputStream(data), metadata)
                .withCannedAcl(CannedAccessControlList.PublicRead)
        )

        return "$baseUrl/$path"
    }

    override fun delete(path: String) {
        s3Client.deleteObject(bucketName, path)
    }
}
```

---

## 4. DTO Updates

### UserDto

```kotlin
data class UserDto(
    @JsonProperty("id")
    val id: String,

    @JsonProperty("first_name")
    val firstName: String,

    @JsonProperty("last_name")
    val lastName: String,

    @JsonProperty("user_name")
    val userName: String,

    @JsonProperty("country_code")
    val countryCode: Int,

    @JsonProperty("phone")
    val phone: String,

    @JsonProperty("profile_picture_url")
    val profilePictureUrl: String? = null,

    @JsonProperty("profile_picture_thumbnail_url")
    val profilePictureThumbnailUrl: String? = null
)
```

---

## 5. Dependencies

Add the following dependencies for image processing:

```kotlin
// build.gradle.kts
dependencies {
    // Image processing
    implementation("net.coobird:thumbnailator:0.4.20")

    // AWS S3 (if using S3 for storage)
    implementation("com.amazonaws:aws-java-sdk-s3:1.12.x")
}
```

---

## 6. Configuration

### application.yml

```yaml
# Profile Picture Settings
profile-picture:
  max-size-bytes: 5242880  # 5MB
  allowed-types:
    - image/jpeg
    - image/png
    - image/webp
  full-size: 512
  thumbnail-size: 256

# Storage (S3 example)
aws:
  s3:
    bucket: ampairs-user-uploads
    base-url: https://storage.ampairs.in
    region: ap-south-1
```

---

## 7. Security Considerations

1. **Authentication**: Endpoint requires valid JWT token
2. **Authorization**: Users can only update their own profile picture
3. **File Validation**:
   - Check file size before processing
   - Validate MIME type
   - Strip EXIF data for privacy
4. **Rate Limiting**: Consider adding rate limits (e.g., 5 uploads per hour)
5. **Storage**: Use signed URLs for private storage if needed

---

## 8. Testing

### cURL Example

```bash
# Upload profile picture
curl -X POST \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -F "file=@/path/to/profile.jpg" \
  https://api.ampairs.in/user/v1/upload-picture
```

### Expected Response

```json
{
  "data": {
    "id": "user-123",
    "first_name": "John",
    "last_name": "Doe",
    "user_name": "johndoe",
    "country_code": 91,
    "phone": "9876543210",
    "profile_picture_url": "https://storage.ampairs.in/users/user-123/profile_1705123456789.jpg",
    "profile_picture_thumbnail_url": "https://storage.ampairs.in/users/user-123/profile_thumb_1705123456789.jpg"
  },
  "error": null
}
```

---

## 9. Mobile App Integration Notes

The mobile app expects:

1. **JSON field names** (snake_case):
   - `profile_picture_url`
   - `profile_picture_thumbnail_url`

2. **Multipart form field name**: `file`

3. **Response format**: Standard `ApiResponse<UserApiModel>` wrapper

4. **Image handling**:
   - Thumbnail URL used in headers/lists (32-64px display)
   - Full URL used in profile screens (120px display)
   - URLs should be publicly accessible (or use CDN)

---

## 10. Rollout Checklist

- [ ] Database migration applied
- [ ] Storage bucket created and configured
- [ ] Profile picture service implemented
- [ ] Upload endpoint deployed
- [ ] User DTO updated with picture fields
- [ ] CORS configured for storage URLs
- [ ] Rate limiting configured
- [ ] Monitoring/logging added
- [ ] API documentation updated
