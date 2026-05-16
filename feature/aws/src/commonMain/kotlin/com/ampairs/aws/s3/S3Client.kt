package com.ampairs.aws.s3

interface S3Client {
    suspend fun getPreSignedUrl(bucketName: String, keyName: String): String
}