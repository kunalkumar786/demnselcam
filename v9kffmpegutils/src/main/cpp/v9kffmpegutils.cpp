#include <jni.h>
#include <string>
#include "logjam.h"


extern "C" {
#include <libavutil/timestamp.h>
#include <libavformat/avformat.h>
#include <libavutil/display.h>
}

void outputLogs() {
    LOGE(3, "error log message");
    LOGW(4, "warning log message");
    LOGI(5, "information log message");
    LOGV(7, "verbose log message");
    LOGDE(8, "default log message");
}

static void log_packet(const AVFormatContext *fmt_ctx, const AVPacket *pkt, const char *tag) {
    AVRational *time_base = &fmt_ctx->streams[pkt->stream_index]->time_base;

    __android_log_print(ANDROID_LOG_INFO, __FUNCTION__,
                        "%s: pts:%s pts_time:%s dts:%s dts_time:%s duration:%s duration_time:%s stream_index:%d\n",
                        tag,
                        av_ts2str(pkt->pts),
                        av_ts2timestr(pkt->pts, time_base),
                        av_ts2str(pkt->dts),
                        av_ts2timestr(pkt->dts, time_base),
                        av_ts2str(pkt->duration),
                        av_ts2timestr(pkt->duration, time_base),
                        pkt->stream_index);

}


void throwException(JNIEnv* pEnv, const char* message) {
    jclass clazz = pEnv->FindClass("java/lang/Exception");
    if (clazz != NULL) {
        pEnv->ThrowNew(clazz, message);
    }
    pEnv->DeleteLocalRef(clazz);
}

static char *jStr2str(JNIEnv *env, jstring source) {
    jsize inputLength = env->GetStringUTFLength(source);
    char *output = new char[inputLength + 1];
    env->GetStringUTFRegion(source, 0, inputLength, output);
    output[inputLength] = '\0';
    return output;
}

/***
 *
 * @param input - the absolute path to file
 * @returns the duration of file in seconds
 *
 */
extern "C"
JNIEXPORT jdouble JNICALL
Java_com_v9k_v9kffmpegutils_SnapshotToolkit_duration(
        JNIEnv *env,
        jobject /* this */,
        jstring input) {

    AVFormatContext* pFormatCtx = NULL;
    if (avformat_open_input(&pFormatCtx, jStr2str(env, input), NULL, NULL) < 0) {
        throwException(env, "Could not open input file");
        return 0;
    }
    if (avformat_find_stream_info(pFormatCtx, NULL) < 0) {
        throwException(env, "Failed to retrieve input stream information");
        return 0;
    }
    int64_t duration = pFormatCtx->duration;
    avformat_close_input(&pFormatCtx);
    avformat_free_context(pFormatCtx);
    return (jdouble) (duration / AV_TIME_BASE);
}


/***
 *
 * Retrieves audio stream from file and video stream from another one and
 * merge these streams to the destination.
 *
 * @param inputV - the absolute path to source video file
 * @param inputA - the absolute path to source audio file
 * @param output - the absolute path to destination file
 *
 *
 */
extern "C"
JNIEXPORT jstring JNICALL
Java_com_v9k_v9kffmpegutils_SnapshotToolkit_merge(
                       JNIEnv *env,
                       jobject /* this */,
                       jstring inputA,
                       jstring inputV,
                       jstring savedPath) {
                       

    AVOutputFormat *ofmt = NULL;
    AVFormatContext *ifmt_ctx1 = NULL, *ifmt_ctx2 = NULL, *ofmt_ctx = NULL;
    AVPacket pkt;

    int i;
    int stream_mapping_size = 0;
    char *in_filename1, *in_filename2;

    in_filename1 = jStr2str(env, inputV);
    in_filename2 = jStr2str(env, inputA);

    const char *out_filename = jStr2str(env, savedPath);

    if (avformat_open_input(&ifmt_ctx1, in_filename1, 0, 0) < 0) {
        throwException(env, "Could not open input file");
    }

    if (avformat_find_stream_info(ifmt_ctx1, 0) < 0) {
        throwException(env, "Failed to retrieve input stream information");
    }

    av_dump_format(ifmt_ctx1, 0, in_filename1, 0);

    avformat_alloc_output_context2(&ofmt_ctx, NULL, NULL, out_filename);
    if (!ofmt_ctx) {
        throwException(env, "Could not create output context");
    }

    ofmt = ofmt_ctx->oformat;

    for (i = 0; i < ifmt_ctx1->nb_streams; i++) {
        AVStream *out_stream;
        AVStream *in_stream = ifmt_ctx1->streams[i];
        AVCodecParameters *in_codecpar = in_stream->codecpar;

        if (in_codecpar->codec_type != AVMEDIA_TYPE_VIDEO) {
            continue;
        }

        out_stream = avformat_new_stream(ofmt_ctx, NULL);
        if (!out_stream) {
            throwException(env, "Failed allocating output stream");
        }

        if (avcodec_parameters_copy(out_stream->codecpar, in_codecpar)< 0) {
            throwException(env, "Failed to copy codec parameters");
        }
        out_stream->codecpar->codec_tag = 0;
    }

    if (avformat_open_input(&ifmt_ctx2, in_filename2, 0, 0) < 0) {
        throwException(env, "Could not open input file");
    }

    if (avformat_find_stream_info(ifmt_ctx2, 0) < 0) {
        throwException(env, "Failed to retrieve input stream information");
    }

    for (i = 0; i < ifmt_ctx2->nb_streams; i++) {
        AVStream *out_stream;
        AVStream *in_stream = ifmt_ctx2->streams[i];
        AVCodecParameters *in_codecpar = in_stream->codecpar;

        if (in_codecpar->codec_type != AVMEDIA_TYPE_AUDIO) {
            continue;
        }

        out_stream = avformat_new_stream(ofmt_ctx, NULL);
        if (!out_stream) {
            throwException(env, "Failed allocating output stream");
        }

        if (avcodec_parameters_copy(out_stream->codecpar, in_codecpar) < 0) {
            throwException(env, "Failed to copy codec parameters");
        }

        out_stream->codecpar->codec_tag = 0;
    }

    av_dump_format(ofmt_ctx, 0, out_filename, 1);

    if (!(ofmt->flags & AVFMT_NOFILE)) {
        if (avio_open(&ofmt_ctx->pb, out_filename, AVIO_FLAG_WRITE) < 0) {
            throwException(env, "Could not open output file in avio_open");
        }
    }

    if (avformat_write_header(ofmt_ctx, NULL) < 0) {
        throwException(env, "Error occurred when opening output file");
    }

    stream_mapping_size = ifmt_ctx2->nb_streams;

    while (av_read_frame(ifmt_ctx2, &pkt) >= 0) {
        AVStream *in_stream, *out_stream;

        in_stream = ifmt_ctx2->streams[pkt.stream_index];

        AVCodecParameters *in_codecpar = in_stream->codecpar;

        if (in_codecpar->codec_type != AVMEDIA_TYPE_AUDIO) {
            av_packet_unref(&pkt);
            continue;
        }

        if (pkt.stream_index >= stream_mapping_size) {
            av_packet_unref(&pkt);
            continue;
        }

        out_stream = ofmt_ctx->streams[pkt.stream_index];

        av_dict_copy(&out_stream->metadata, in_stream->metadata, AV_DICT_DONT_OVERWRITE);

        /* copy packet */
        pkt.pts = av_rescale_q_rnd(pkt.pts, in_stream->time_base, out_stream->time_base, AV_ROUND_NEAR_INF);
        pkt.dts = av_rescale_q_rnd(pkt.dts, in_stream->time_base, out_stream->time_base, AV_ROUND_NEAR_INF);
        pkt.duration = av_rescale_q(pkt.duration, in_stream->time_base, out_stream->time_base);
        pkt.pos = -1;

        int ret = av_interleaved_write_frame(ofmt_ctx, &pkt);
        if (ret < 0) {
            __android_log_print(ANDROID_LOG_INFO, __FUNCTION__, "In merge function: Error muxing packet %s\n", av_err2str(ret));
            break;
        }
        av_packet_unref(&pkt);
    }

    stream_mapping_size = ifmt_ctx1->nb_streams;

    while (av_read_frame(ifmt_ctx1, &pkt) >= 0) {
        AVStream *in_stream, *out_stream;

        in_stream = ifmt_ctx1->streams[pkt.stream_index];

        AVCodecParameters *in_codecpar = in_stream->codecpar;

        if (in_codecpar->codec_type != AVMEDIA_TYPE_VIDEO) {
            av_packet_unref(&pkt);
            continue;
        }

        if (pkt.stream_index >= stream_mapping_size) {
            av_packet_unref(&pkt);
            continue;
        }

        out_stream = ofmt_ctx->streams[pkt.stream_index];

        av_dict_copy(&out_stream->metadata, in_stream->metadata, AV_DICT_DONT_OVERWRITE);


        /* copy packet */
        pkt.pts = av_rescale_q_rnd(pkt.pts, in_stream->time_base, out_stream->time_base, AV_ROUND_NEAR_INF);
        pkt.dts = av_rescale_q_rnd(pkt.dts, in_stream->time_base, out_stream->time_base, AV_ROUND_NEAR_INF);
        pkt.duration = av_rescale_q(pkt.duration, in_stream->time_base, out_stream->time_base);
        pkt.pos = -1;

        int ret = av_interleaved_write_frame(ofmt_ctx, &pkt);
        if (ret < 0) {
            __android_log_print(ANDROID_LOG_INFO, __FUNCTION__, "In other merge function: Error muxing packet %s\n", av_err2str(ret));
            break;
        }
        av_packet_unref(&pkt);
    }


    av_write_trailer(ofmt_ctx);

    avformat_close_input(&ifmt_ctx1);
    avformat_close_input(&ifmt_ctx2);

    /* close output */
    if (ofmt_ctx && !(ofmt->flags & AVFMT_NOFILE))
        avio_closep(&ofmt_ctx->pb);
    avformat_free_context(ofmt_ctx);

    return env->NewStringUTF(out_filename);
}

/***
 *
 * Trims an input video according to start and end params and write the
 * result video to the destination file.
 *
 * @param input - the absolute path to source the video file
 * @param output - the absolute path to the destination file
 * @param start - start second
 * @param end - end second
 *
 *
 */
extern "C"
JNIEXPORT jstring JNICALL
Java_com_v9k_v9kffmpegutils_SnapshotToolkit_trim(
                       JNIEnv *env,
                       jobject /* this */,
                       jdouble start,
                       jdouble end,
                       jstring input,
                       jstring savedPath) {
    AVOutputFormat *ofmt = NULL;
    AVFormatContext *ifmt_ctx = NULL, *ofmt_ctx = NULL;
    AVPacket pkt;

    int ret, i;
    int stream_mapping_size = 0;
    char *in_filename;
    in_filename = jStr2str(env, input);
    const char *out_filename = jStr2str(env, savedPath);

    LOGDE(8, "output filename: %s ", out_filename);

    LOGDE(8, "end time: %f ", end);


    if ((ret = avformat_open_input(&ifmt_ctx, in_filename, 0, 0)) < 0) {
        throwException(env, "Could not open input file");
    }

    if ((ret = avformat_find_stream_info(ifmt_ctx, 0)) < 0) {
        throwException(env, "Failed to retrieve input stream information");
    }

    av_dump_format(ifmt_ctx, 0, in_filename, 0);


    avformat_alloc_output_context2(&ofmt_ctx, NULL, NULL, out_filename);

    if (!ofmt_ctx) {
        throwException(env, "Could not create output context");
        ret = AVERROR_UNKNOWN;
    }

    stream_mapping_size = ifmt_ctx->nb_streams;

    LOGDE(8, "stream_mapping_size: %d ", stream_mapping_size);

    ofmt = ofmt_ctx->oformat;

    for (i = 0; i < ifmt_ctx->nb_streams; i++) {
        AVStream *out_stream;
        AVStream *in_stream = ifmt_ctx->streams[i];
        AVCodecParameters *in_codecpar = in_stream->codecpar;

        if (in_codecpar->codec_type != AVMEDIA_TYPE_AUDIO &&
            in_codecpar->codec_type != AVMEDIA_TYPE_VIDEO &&
            in_codecpar->codec_type != AVMEDIA_TYPE_SUBTITLE) {
            continue;
        }

        out_stream = avformat_new_stream(ofmt_ctx, NULL);
        if (!out_stream) {
            throwException(env, "Failed allocating output stream\n");
            ret = AVERROR_UNKNOWN;
        }

        ret = avcodec_parameters_copy(out_stream->codecpar, in_codecpar);
        if (ret < 0) {
            throwException(env, "Failed to copy codec parameters");
        }
        out_stream->codecpar->codec_tag = 0;
    }
    av_dump_format(ofmt_ctx, 0, out_filename, 1);

    if (!(ofmt->flags & AVFMT_NOFILE)) {
        ret = avio_open(&ofmt_ctx->pb, out_filename, AVIO_FLAG_WRITE);
        LOGDE(8, "avio_open: %s", av_err2str(ret));
        if (ret < 0) {
            throwException(env, "Could not open output file in avio_open");
        }
    }

    ret = avformat_write_header(ofmt_ctx, NULL);
    if (ret < 0) {
        throwException(env, "Error occurred when opening output file");
    }

    int64_t *dts_start_from = (int64_t *) malloc(sizeof(int64_t) * ifmt_ctx->nb_streams);
    memset(dts_start_from, 0, sizeof(int64_t) * ifmt_ctx->nb_streams);
    int64_t *pts_start_from = (int64_t *) malloc(sizeof(int64_t) * ifmt_ctx->nb_streams);
    memset(pts_start_from, 0, sizeof(int64_t) * ifmt_ctx->nb_streams);

    while (av_read_frame(ifmt_ctx, &pkt) >= 0) {
        AVStream *in_stream, *out_stream;

        in_stream = ifmt_ctx->streams[pkt.stream_index];

        if (pkt.stream_index >= stream_mapping_size) {
            av_packet_unref(&pkt);
            continue;
        }

        double currentTime = av_q2d(in_stream->time_base) * pkt.pts;

        if (pkt.pts == AV_NOPTS_VALUE) {
            pkt.pts = pkt.dts;
        }

        if (currentTime < start || currentTime > end) {
            av_packet_unref(&pkt);
            continue;
        }

        if (dts_start_from[pkt.stream_index] == 0) {
            dts_start_from[pkt.stream_index] = pkt.dts;
        }
        if (pts_start_from[pkt.stream_index] == 0) {
            pts_start_from[pkt.stream_index] = pkt.pts;
        }


        out_stream = ofmt_ctx->streams[pkt.stream_index];
        log_packet(ifmt_ctx, &pkt, "in");

        av_dict_copy(&out_stream->metadata, in_stream->metadata, AV_DICT_DONT_OVERWRITE);

        
        /* copy packet */
        pkt.pts = av_rescale_q_rnd(pkt.pts - pts_start_from[pkt.stream_index], in_stream->time_base, out_stream->time_base, (AVRounding) (AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX));
        pkt.dts = av_rescale_q_rnd(pkt.dts - dts_start_from[pkt.stream_index], in_stream->time_base, out_stream->time_base, (AVRounding) (AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX));

        if (pkt.pts < 0) {
            pkt.pts = 0;
        }
        if (pkt.dts < 0) {
            pkt.dts = 0;
        }

        pkt.duration = (int) av_rescale_q((int64_t) pkt.duration, in_stream->time_base, out_stream->time_base);

        pkt.pos = -1;
        log_packet(ofmt_ctx, &pkt, "out");

        ret = av_interleaved_write_frame(ofmt_ctx, &pkt);
        if (ret < 0) {
            __android_log_print(ANDROID_LOG_INFO, __FUNCTION__, "In trim function: Error muxing packet\n");
            break;
        }

        av_packet_unref(&pkt);
    }

    free(dts_start_from);
    free(pts_start_from);

    av_write_trailer(ofmt_ctx);
    
    avformat_close_input(&ifmt_ctx);

    /* close output */
    if (ofmt_ctx && !(ofmt->flags & AVFMT_NOFILE))
        avio_closep(&ofmt_ctx->pb);
    avformat_free_context(ofmt_ctx);

    if (ret < 0 && ret != AVERROR_EOF) {
        __android_log_print(ANDROID_LOG_INFO, __FUNCTION__, "Error occurred: %s\n",
                            av_err2str(ret));
    }

    return env->NewStringUTF(out_filename);
}
