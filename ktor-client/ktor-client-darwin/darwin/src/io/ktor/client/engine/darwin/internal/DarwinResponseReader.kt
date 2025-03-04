/*
 * Copyright 2014-2022 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.engine.darwin.internal

import io.ktor.client.engine.darwin.*
import io.ktor.client.request.*
import io.ktor.client.utils.*
import io.ktor.http.*
import io.ktor.util.collections.*
import io.ktor.util.date.*
import io.ktor.utils.io.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import platform.Foundation.*
import platform.darwin.*
import kotlin.coroutines.*

@OptIn(UnsafeNumber::class)
internal class DarwinResponseReader(
    private val config: DarwinClientEngineConfig
) : NSObject(), NSURLSessionDataDelegateProtocol {
    private val taskHandlers = ConcurrentMap<NSURLSessionTask, DarwinTaskHandler>()

    override fun URLSession(session: NSURLSession, dataTask: NSURLSessionDataTask, didReceiveData: NSData) {
        val taskHandler = taskHandlers[dataTask] ?: return
        taskHandler.receiveData(dataTask, didReceiveData)
    }

    override fun URLSession(session: NSURLSession, task: NSURLSessionTask, didCompleteWithError: NSError?) {
        val taskHandler = taskHandlers[task] ?: return
        taskHandler.complete(task, didCompleteWithError)
        taskHandlers.remove(task)
    }

    fun read(
        request: HttpRequestData,
        callContext: CoroutineContext,
        task: NSURLSessionTask
    ): CompletableDeferred<HttpResponseData> {
        val taskHandler = DarwinTaskHandler(request, callContext)
        taskHandlers.put(task, taskHandler)
        return taskHandler.response
    }

    /**
     * Disable embedded redirects.
     */
    override fun URLSession(
        session: NSURLSession,
        task: NSURLSessionTask,
        willPerformHTTPRedirection: NSHTTPURLResponse,
        newRequest: NSURLRequest,
        completionHandler: (NSURLRequest?) -> Unit
    ) {
        completionHandler(null)
    }

    /**
     * Handle challenge.
     */
    override fun URLSession(
        session: NSURLSession,
        task: NSURLSessionTask,
        didReceiveChallenge: NSURLAuthenticationChallenge,
        completionHandler: (NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Unit
    ) {
        val handler = config.challengeHandler
        if (handler != null) {
            handler(session, task, didReceiveChallenge, completionHandler)
        } else {
            completionHandler(NSURLSessionAuthChallengePerformDefaultHandling, didReceiveChallenge.proposedCredential)
        }
    }
}
