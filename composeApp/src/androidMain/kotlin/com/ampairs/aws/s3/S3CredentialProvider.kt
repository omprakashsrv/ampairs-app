package com.ampairs.aws.s3

import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.collections.Attributes

private const val PROVIDER_NAME = "SystemProperties"

private val ACCESS_KEY_ID = "AKIA2N6MZHTD5YV4LNMC"
private val SECRET_ACCESS_KEY = "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"
private val SESSION_TOKEN = ""

/**
 * A [CredentialsProvider] which reads `aws.accessKeyId`, `aws.secretAccessKey`, and `aws.sessionToken`.
 */
class S3CredentialProvider : CredentialsProvider {

    override suspend fun resolve(attributes: Attributes): Credentials {
        return Credentials(
            accessKeyId = ACCESS_KEY_ID,
            secretAccessKey = SECRET_ACCESS_KEY,
            sessionToken = SESSION_TOKEN,
            providerName = PROVIDER_NAME,
        )
    }
}