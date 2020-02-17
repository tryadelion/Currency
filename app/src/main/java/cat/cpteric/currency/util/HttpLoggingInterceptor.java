package cat.cpteric.currency.util;

import android.util.Log;

import androidx.annotation.Nullable;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.http.HttpHeaders;
import okio.Buffer;
import okio.BufferedSource;

/**
 * An OkHttp interceptor which logs request and response information. Can be applied as an
 * {@linkplain OkHttpClient#interceptors() application interceptor} or as a {@linkplain
 * OkHttpClient#networkInterceptors() network interceptor}. <p> The format of the logs created by
 * this class should not be considered stable and may change slightly between releases. If you need
 * a stable logging format, use your own interceptor.
 */

public final class HttpLoggingInterceptor
        implements Interceptor {
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private final int BYTE_COUNT = 1024;
    private final String FORMAT_HEADER_AND_BODY = "%n------%nHeaders%n------%s%n------%nBody: %s%n------";
    private final String FORMAT_HEADER_ONLY = "%n------%nHeaders%n------%s%n------";
    private volatile int mRequestId;
    private Level mLevel = Level.NONE;
    // endregion
    private String mTag = "HTTP";

    public HttpLoggingInterceptor(String tag, Level level) {
        setTag(tag);
        setLevel(level);
    }

    // region level
    public enum Level {
        /**
         * No logs.
         */
        NONE,
        /**
         * Logs request and response lines.
         * <p>
         * <p>Example:
         * <pre>{@code
         * --> POST /greeting http/1.1 (3-byte body)
         *
         * <-- 200 OK (22ms, 6-byte body)
         * }</pre>
         */
        BASIC,
        /**
         * Logs request and response lines and their respective headers.
         * <p>
         * <p>Example:
         * <pre>{@code
         * --> POST /greeting http/1.1
         * Host: example.com
         * Content-Type: plain/text
         * Content-Length: 3
         * --> END POST
         *
         * <-- 200 OK (22ms)
         * Content-Type: plain/text
         * Content-Length: 6
         * <-- END HTTP
         * }</pre>
         */
        HEADERS,
        /**
         * Logs request and response lines and their respective headers and bodies (if present).
         * <p>
         * <p>Example:
         * <pre>{@code
         * --> POST /greeting http/1.1
         * Host: example.com
         * Content-Type: plain/text
         * Content-Length: 3
         *
         * Hi?
         * --> END POST
         *
         * <-- 200 OK (22ms)
         * Content-Type: plain/text
         * Content-Length: 6
         *
         * Hello!
         * <-- END HTTP
         * }</pre>
         */
        BODY
    }

    /**
     * Change the tag at which this interceptor logs.
     */
    private HttpLoggingInterceptor setTag(@Nullable final String tag) {
        if (tag != null) {
            mTag = tag;
        }
        return this;
    }

    /**
     * Change the level at which this interceptor logs.
     */
    private HttpLoggingInterceptor setLevel(Level level) {
        if (level == null) {
            throw new NullPointerException("level == null. Use Level.NONE instead.");
        }
        mLevel = level;
        return this;
    }

    @Override
    public Response intercept(Chain chain)
            throws IOException {
        Request request = chain.request();
        if (mLevel == Level.NONE) {
            return chain.proceed(request);
        }

        mRequestId++;
        boolean logBody = mLevel == Level.BODY;
        boolean logHeaders = logBody || mLevel == Level.HEADERS;

        RequestBody requestBody = request.body();
        boolean hasRequestBody = requestBody != null;

        StringBuilder header = new StringBuilder();
        StringBuilder body = new StringBuilder();
        if (logHeaders) {
            if (hasRequestBody) {
                // Request body headers are only present when installed as a network interceptor. Force
                // them to be included (when available) so there values are known.
                if (requestBody.contentType() != null) {
                    header.append("\nContent-Type: ").append(requestBody.contentType());
                }
                if (requestBody.contentLength() != -1) {
                    header.append("\nContent-Length: ").append(requestBody.contentLength());
                }
            }

            Headers headers = request.headers();
            for (int i = 0, count = headers.size(); i < count; i++) {
                String name = headers.name(i);
                // Skip headers from the request body as they are explicitly logged above.
                if (!"Content-Type".equalsIgnoreCase(name) && !"Content-Length".equalsIgnoreCase(name)) {
                    header.append("\n").append(name).append(": ").append(headers.value(i));
                }
            }

            if (!logBody || !hasRequestBody) {
                body.append("no body");
            } else if (bodyEncoded(request.headers())) {
                body.append("encoded body omitted");
            } else {
                Buffer buffer = new Buffer();
                requestBody.writeTo(buffer);

                Charset charset = UTF8;
                MediaType contentType = requestBody.contentType();
                if (contentType != null) {
                    charset = contentType.charset(UTF8);
                }

                if (isPlaintext(buffer) && charset != null) {
                    body.append(buffer.readString(charset));
                }
            }
        }

        if (logBody) {
            Log.d(mTag, String.format("#" + mRequestId + " Sending %s request %s" + FORMAT_HEADER_AND_BODY, request.method(), request.url(), header, body));
        } else {
            Log.d(mTag, String.format("#" + mRequestId + " Sending %s request %s" + FORMAT_HEADER_ONLY, request.method(), request.url(), header));
        }
        long startNs = System.nanoTime();
        Response response;
        try {
            response = chain.proceed(request);
        } catch (Exception e) {
            // ignore canceled requests
            if (e instanceof IOException && "Canceled".equalsIgnoreCase(e.getMessage())) {
                throw e;
            }
            Log.w(mTag, String.format(Locale.ENGLISH, "Response failed for %s", request.url()));
            Log.w(mTag, e);
            throw e;
        }
        long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);

        header = new StringBuilder();
        body = new StringBuilder();

        ResponseBody responseBody = response.body();
        long contentLength = responseBody.contentLength();

        if (logHeaders) {
            Headers headers = response.headers();
            for (int i = 0, count = headers.size(); i < count; i++) {
                header.append("\n").append(headers.name(i)).append(": ").append(headers.value(i));
            }

            if (logBody && HttpHeaders.hasBody(response) && !bodyEncoded(response.headers())) {
                try {
                    BufferedSource source = responseBody.source();
                    source.request(Integer.MAX_VALUE);
                    Buffer buffer = source.buffer();

                    Charset charset = UTF8;
                    MediaType contentType = responseBody.contentType();
                    if (contentType != null) {
                        charset = contentType.charset(UTF8);
                    }

                    if (!isPlaintext(buffer)) {
                        body.append("(binary ").append(buffer.size()).append("-byte body omitted)");
                        return response;
                    }

                    if (contentLength != 0) {
                        Buffer clonedBody = buffer.clone();
                        if (clonedBody.size() > BYTE_COUNT) {
                            body.append(clonedBody.readString(BYTE_COUNT, charset));
                            body.append("...");
                        } else {
                            body.append(clonedBody.readString(charset));
                        }
                    }
                } catch (IllegalStateException ise) {
                    Log.w(mTag, String.format(Locale.ENGLISH, "#" + mRequestId + " Received response for %s with %s in %dms" + FORMAT_HEADER_AND_BODY,
                            response.request().url(),
                            response.code(),
                            tookMs,
                            header,
                            "Internal Error! BufferedSource was closed!"));
                    return response;
                }
            }
        }
        if (logBody) {
            Log.d(mTag, String.format(Locale.ENGLISH, "#" + mRequestId + " Received response for %s with %s in %dms" + FORMAT_HEADER_AND_BODY,
                    response.request().url(),
                    response.code(),
                    tookMs,
                    header,
                    body));
        } else {
            Log.d(mTag, String.format(Locale.ENGLISH, "#" + mRequestId + " Received response for %s with %s in %dms" + FORMAT_HEADER_ONLY,
                    response.request().url(),
                    response.code(),
                    tookMs,
                    header));
        }
        return response;
    }

    /**
     * Returns true if the body in question probably contains human readable text. Uses a small sample
     * of code points to detect unicode control characters commonly used in binary file signatures.
     */
    private boolean isPlaintext(Buffer buffer) {
        try {
            Buffer prefix = new Buffer();
            long byteCount = buffer.size() < 64 ? buffer.size() : 64;
            buffer.copyTo(prefix, 0, byteCount);
            for (int i = 0; i < 16; i++) {
                if (prefix.exhausted()) {
                    break;
                }
                int codePoint = prefix.readUtf8CodePoint();
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                    return false;
                }
            }
            return true;
        } catch (EOFException e) {
            return false; // Truncated UTF-8 sequence.
        }
    }

    private boolean bodyEncoded(Headers headers) {
        String contentEncoding = headers.get("Content-Encoding");
        return contentEncoding != null && !contentEncoding.equalsIgnoreCase("identity");
    }
}
