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
import kotlinx.coroutines.flow.flow

/**
 * Make a network request and emit the response. Additionally, takes an action to perform
 * if a network request fails.
 * @author Rajesh Hadiya
 * @param makeNetworkRequest - A function to make network request
 * @param onNetworkRequestFailed - An action to perform when a network request fails
 * @return [REMOTE] type
 */
inline fun <REMOTE> networkResource(
    crossinline shouldMakeNetworkRequest: () -> Boolean = { true },
    crossinline makeNetworkRequest: suspend () -> Flow<Response<REMOTE>>,
    crossinline processNetworkResponse: suspend (response: REMOTE) -> Unit = { },
) = flow<Resource<REMOTE>> {
    emit(Resource.loading(data = null))
    if (shouldMakeNetworkRequest()) {
        makeNetworkRequest().collect { remoteResponse ->
            val response = remoteResponse.data
            if (remoteResponse.error != null || response == null) {
                emit(
                    Resource.error(
                        errorMessage = remoteResponse.error?.message ?: "",
                        errorCode = remoteResponse.error?.code ?: "",
                        data = null
                    )
                )
            } else {
                processNetworkResponse(response)
                emit(Resource.success(data = response))
            }
        }
    }
}
