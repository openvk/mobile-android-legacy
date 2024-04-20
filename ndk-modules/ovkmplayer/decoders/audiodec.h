//
// Created by tretdm on 20.04.2024.
//

#ifndef MOBILE_ANDROID_LEGACY_AUDIODEC_H
#define MOBILE_ANDROID_LEGACY_AUDIODEC_H

#include <../utils/pktqueue.h>

typedef void (*DecoderHandler) (short*, int);

// FFmpeg implementation headers (using LGPLv3.0 model)
extern "C" {
    #define __STDC_CONSTANT_MACROS          // workaround for compiler
    #include <libavutil/avstring.h>
    #include <libavutil/pixdesc.h>
    #include <libavutil/imgutils.h>
    #include <libavutil/samplefmt.h>
    #include <libavformat/avformat.h>
    #include <libavformat/url.h>
    #include <libavformat/avio.h>
    #include <libswscale/swscale.h>
    #include <libavcodec/avcodec.h>
    #include <libavcodec/avfft.h>
    #include <libavdevice/avdevice.h>
    #include <libswresample/swresample.h>
}

class AudioDecoder {
    public:
        AudioDecoder(AVStream* pStream, PacketQueue *pPktQueue);
        bool                prepare();
        bool                process();
        bool                decode(void *ptr);
        DecoderHandler		onDecode;
        int                 gBufferSize;
        short*              gBuffer;
        bool                gRunning;
    private:
        PacketQueue*        gPktQueue;
};



#endif //MOBILE_ANDROID_LEGACY_AUDIODEC_H
