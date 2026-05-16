package com.ampairs.aws.s3

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.presigners.presignGetObject
import kotlin.time.Duration.Companion.hours

class AwsS3Client : com.ampairs.aws.s3.S3Client {
    private var s3Client: S3Client

    init {
        s3Client = S3Client {
            region = "ap-south-1"
            credentialsProvider = S3CredentialProvider()
        }
    }

    override suspend fun getPreSignedUrl(bucketName: String, keyName: String): String {
//         Create a GetObjectRequest.
        val unsignedRequest = GetObjectRequest {
            bucket = bucketName
            key = keyName
        }
//         Presign the GetObject request.
        val presignedRequest = s3Client.presignGetObject(unsignedRequest, 1.hours)
        return presignedRequest.url.toString()
    }
}