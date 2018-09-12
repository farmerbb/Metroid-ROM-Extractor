/*
 * Based on code by FIX94
 * See https://gist.github.com/FIX94/7593640c5cee6c37e3b23e7fcf8fe5b7
 */

#include <stdio.h>
#include <malloc.h>
#include <memory.h>
#include <jni.h>

static int METROID_FDS = 0;
static int METROID_NES = 1;

static int decBit;
static unsigned char *rPos;
static unsigned int tmpBuf;
static size_t position;
// gets read position for out_buf
static void runDecBit()
{
    if(position == 0)
    {
        position = 8;
        tmpBuf = *rPos++;
    }
    decBit <<= 1;
    if(tmpBuf & 0x80)
        decBit |= 1;
    tmpBuf <<= 1;
    position--;
}

unsigned char* decrypt_metroid(unsigned char *in_buf, int metroid_version) {
    int decLen, xorLen, xorVal;
    unsigned char decByte;
    // init data for decryption
    if(metroid_version == METROID_FDS)
    {
        decLen = 0x1AB00;
        xorLen = 0x1AAFC;
        xorVal = 0xB1B2;
        decByte = 0x9D;
    }
    else
    {
        decLen = 0x20000;
        xorLen = 0x1FFFC;
        xorVal = 0xA663;
        decByte = 0xE9;
    }
    int i, j;
    // simple add obfuscation
    for(i = 0; i < 0x100; i++)
    {
        in_buf[i] += decByte;
        decByte = in_buf[i];
    }
    // flip the first 0x100 bytes around
    for(i = 0; i < 0x80; i++)
    {
        unsigned char tmpVal = in_buf[i];
        in_buf[i] = in_buf[0xFF - i];
        in_buf[0xFF - i] = tmpVal;
    }
    // set up buffer pointers
    unsigned char *outBuf = malloc(decLen);
    memset(outBuf, 0, decLen);
    rPos = in_buf + 0x100;
    position = 0;
    tmpBuf = 0;
    // unscramble buffer
    for(i = 0; i < decLen; i++)
    {
        decBit = 0;
        runDecBit();
        if(decBit)
        {
            decBit = 0;
            for(j = 0; j < 8; j++)
                runDecBit();
            outBuf[i] = in_buf[decBit + 0x49];
        }
        else
        {
            decBit = 0;
            runDecBit();
            if(decBit)
            {
                decBit = 0;
                for(j = 0; j < 6; j++)
                    runDecBit();
                outBuf[i] = in_buf[decBit + 9];
            }
            else
            {
                decBit = 0;
                runDecBit();
                if(decBit)
                {
                    decBit = 0;
                    for(j = 0; j < 3; j++)
                        runDecBit();
                    outBuf[i] = in_buf[decBit + 1];
                }
                else
                    outBuf[i] = in_buf[decBit];
            }
        }
    }
    // do checksum fixups
    unsigned int xorTmpVal = 0;
    for(i = 0; i < xorLen; i++)
    {
        xorTmpVal ^= outBuf[i];
        for(j = 0; j < 8; j++)
        {
            if(xorTmpVal & 1)
            {
                xorTmpVal >>= 1;
                xorTmpVal ^= xorVal;
            }
            else
                xorTmpVal >>= 1;
        }
    }
    // write in calculated checksum
    outBuf[xorLen - 1] = (xorTmpVal >> 8) & 0xFF;
    outBuf[xorLen - 2] = xorTmpVal & 0xFF;

    return outBuf;
}

jbyteArray as_byte_array(JNIEnv* env, unsigned char* buf, int len) {
    jbyteArray array = (*env)->NewByteArray(env, len);
    (*env)->SetByteArrayRegion(env, array, 0, len, (jbyte*)(buf));
    return array;
}

unsigned char* as_unsigned_char_array(JNIEnv* env, jbyteArray array) {
    int len = (*env)->GetArrayLength(env, array);
    unsigned char* buf = malloc(len);
    (*env)->GetByteArrayRegion(env, array, 0, len, (jbyte*)(buf));
    return buf;
}

JNIEXPORT jbyteArray JNICALL
Java_com_farmerbb_metroidromextractor_Extractor_decryptMetroidNative(
        JNIEnv* env,
        jobject thiz,
        jbyteArray array) {
    unsigned char *inArray = as_unsigned_char_array(env, array);
    unsigned char *outArray = decrypt_metroid(inArray, METROID_NES);

    return as_byte_array(env, outArray, 0x20000);
}