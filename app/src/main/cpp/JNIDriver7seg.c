#include <stdio.h>
#include <unistd.h>
#include <fcntl.h>
#include <jni.h>
int fd=0;

JNIEXPORT jint JNICALL
Java_parlab_example_slidepuzzle_MainActivity_openDriver7seg
        (JNIEnv *env, jclass class, jstring path)
{
    jboolean iscopy;
    const char *path_utf=(*env)->GetStringUTFChars(env, path, &iscopy);
    fd=open(path_utf, O_WRONLY);
    (*env)->ReleaseStringUTFChars(env, path, path_utf);

    if(fd<0) return -1;
    else return 1;
}

JNIEXPORT void JNICALL
Java_parlab_example_slidepuzzle_MainActivity_closeDriver7seg
(JNIEnv *env, jclass class)
{
    if(fd>0) close(fd);
}

JNIEXPORT void JNICALL
Java_parlab_example_slidepuzzle_MainActivity_writeDriver7seg
(JNIEnv *env, jclass class, jbyteArray arr, jint count)
{
    jbyte * chars=(*env)->GetByteArrayElements(env, arr, 0);
    if(fd>0) write(fd, (unsigned char*)chars, count);
    (*env)->ReleaseByteArrayElements(env, arr, chars, 0);
}