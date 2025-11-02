package com.ampairs.aws.s3

class IosS3Client : S3Client {

    override suspend fun getPreSignedUrl(bucketName: String, keyName: String): String {
        // TODO: Implement AWS S3 client for iOS using AWS SDK for iOS
        // For now, return a placeholder URL
        return "https://$bucketName.s3.amazonaws.com/$keyName"
    }
}