#ifndef LOGJAM_H
#define LOGJAM_H

#include <android/log.h>

#define LOGTAG "v9kffmpegutils"
#define LOG_LEVEL 9

#define LOGV(level, ...) if(level <= LOG_LEVEL) { __android_log_print(ANDROID_LOG_VERBOSE, LOGTAG, __VA_ARGS__);}

#define LOGDE(level, ...) if(level <= LOG_LEVEL) { __android_log_print(ANDROID_LOG_DEBUG, LOGTAG, __VA_ARGS__); }

#define LOGI(level, ...) if(level <= LOG_LEVEL) { __android_log_print(ANDROID_LOG_INFO, LOGTAG, __VA_ARGS__); }

#define LOGW(level, ...) if(level <= LOG_LEVEL) { __android_log_print(ANDROID_LOG_WARN, LOGTAG, __VA_ARGS__); }

#define LOGE(level, ...) if(level <= LOG_LEVEL) { __android_log_print(ANDROID_LOG_ERROR, LOGTAG, __VA_ARGS__); }

#endif
