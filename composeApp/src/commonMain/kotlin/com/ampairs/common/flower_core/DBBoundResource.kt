/*
 *  Copyright (C) 2022 Rajesh Hadiya
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.ampairs.common.flower_core

import com.ampairs.common.model.Response
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

/**
 * Fetch the data from local database (if available), perform a network request (if instructed)
 * and emit the response after saving it to local database.
 * Additionally, takes an action to perform if a network request fails.
 * @author Rajesh Hadiya
 * @param fetchFromLocal - A function to retrieve data from local database
 * @param shouldMakeNetworkRequest - Whether or not to make network request
 * @param makeNetworkRequest - A function to make network request
 * @param processNetworkResponse - A function to process network response (e.g., saving response headers before saving actual data)
 * @param saveResponseData - A function to save network response
 * @param onNetworkRequestFailed - An action to perform when a network request fails
 * @return [DB] type
 */
inline fun <DB, REMOTE> dbBoundResource(
    crossinline fetchFromLocal: () -> Flow<DB>,
    crossinline shouldMakeNetworkRequest: (DB?) -> Boolean = { true },
    crossinline makeNetworkRequest: suspend () -> Flow<Response<REMOTE>>,
    crossinline processNetworkResponse: suspend (response: REMOTE) -> Unit = { },
    crossinline saveResponseData: (REMOTE) -> Unit = { },
    crossinline onNetworkRequestFailed: (errorMessage: String, httpStatusCode: Int) -> Unit = { _: String, _: Int -> },
) = flow<Resource<DB>> {
    emit(Resource.loading(data = null))
    val localFetch = fetchFromLocal()
    val localData = localFetch.first()
    if (shouldMakeNetworkRequest(localData)) {
        emit(Resource.loading(data = localData))
        makeNetworkRequest().collect { remoteResponse ->
            val response = remoteResponse.data
            if (remoteResponse.error != null || response == null) {
                emit(
                    Resource.error(
                        errorMessage = remoteResponse.error?.message ?: "",
                        errorCode = remoteResponse.error?.code ?: "",
                        data = localData
                    )
                )
            } else {
                processNetworkResponse(response)
                saveResponseData(response)
                emit(Resource.success(data = fetchFromLocal().first()!!))
            }
        }
    } else {
        emit(Resource.success(data = localData!!))
    }
}
