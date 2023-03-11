#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/time.h>
#include <android/log.h>
#include <android/bitmap.h>
#include <jni.h>
#include <CL/opencl.h>
#include "ImageProcessing.h"
#include <jni.h>

#define checkCL(expression) {                        \
	cl_int err = (expression);                       \
	if (err < 0 && err > -64) {                      \
		__android_log_print(ANDROID_LOG_ERROR,       \
		       "LOG_TAG",                            \
		       "Error on line %d. error code: %d\n", \
				__LINE__, err);                      \
	}                                                \
}
#define LOG_TAG "DEBUG"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

JNIEXPORT jobject JNICALL
Java_parlab_example_slidepuzzle_MainActivity_puzzleGPU(JNIEnv *env, jclass class,
                                                       jobject bitmap, jintArray puzzleMask) {
    FILE *file_handle;
    char *kernel_file_buffer, *file_log;
    size_t kernel_file_size, log_size;

    unsigned char* cl_file_name = "/data/local/tmp/Project.cl";
    unsigned char* kernel_name = "kernel_puzzle";

    // Device input buffers
    cl_mem d_src;
    // Device output buffer
    cl_mem d_dst;

    cl_platform_id cpPlatform;        // OpenCL platform
    cl_device_id device_id;           // device ID
    cl_context context;               // context
    cl_command_queue queue;           // command queue
    cl_program program;               // program
    cl_kernel kernel;                 // kernel

    file_handle=fopen(cl_file_name, "r");
    if(file_handle==NULL)
    {
        printf("Couldn't find the file");
        exit(1);
    }

    //read kernel file
    fseek(file_handle, 0, SEEK_END);
    kernel_file_size =ftell(file_handle);
    rewind(file_handle);
    kernel_file_buffer = (char*)malloc(kernel_file_size+1);//TODO
    kernel_file_buffer[kernel_file_size]='\0';
    fread(kernel_file_buffer, sizeof(char), kernel_file_size, file_handle);
    fclose(file_handle);

    //getting bitmap info:
    LOGD("reading bitmap info...");
    AndroidBitmapInfo info;
    int ret;
    if((ret=AndroidBitmap_getInfo(env, bitmap, &info))<0){
        LOGE("AndroidBitmap_getInfo() failed ! error = %d", ret);
        return NULL;
    }

    if(info.format != ANDROID_BITMAP_FORMAT_RGBA_8888){
        LOGE("Bitmap format is not RGBA_8888");
        return NULL;
    }

    //read pixels of bitmap into native memory:
    LOGD("reading bitmap pixels...");
    void* bitmapPixels;
    if((ret=AndroidBitmap_lockPixels(env, bitmap, &bitmapPixels))<0){
        LOGE("AndroidBitmap_lockPixels() failed != error=%d", ret);
        return NULL;
    }
    LOGD("width:%d height:%d stride:%d", info.width, info.height, info.stride);
    uint32_t* src = (uint32_t*) bitmapPixels;
    uint32_t* tempPixels = (uint32_t*)malloc(info.height * info.width * 4);

    int pixelsCount = info.height * info.width;
    memcpy(tempPixels, src, sizeof(uint32_t) * pixelsCount);

    size_t globalSize, localSize, grid;
    localSize=64;
    grid = (pixelsCount%localSize) ? (pixelsCount/localSize)+1 : pixelsCount/localSize;
    globalSize = grid*localSize;

    cl_int err;
    // Bind to platform
    checkCL(clGetPlatformIDs(1, &cpPlatform, NULL));

    // Get ID for the device
    checkCL(clGetDeviceIDs(cpPlatform, CL_DEVICE_TYPE_GPU, 1, &device_id, NULL));

    // Create a context
    context = clCreateContext(0, 1, &device_id, NULL, NULL, &err);

    // Create a command queue
    queue = clCreateCommandQueue(context, device_id, 0, &err);
    checkCL(err);

    // Create the compute program from the source buffer
    program = clCreateProgramWithSource(context, 1, (const char **) & kernel_file_buffer, &kernel_file_size, &err);
    checkCL(err);

    // Build the program executable
    checkCL(clBuildProgram(program, 0, NULL, NULL, NULL, NULL));

    /*size_t len = 0;
    cl_int ret1 = CL_SUCCESS;
    ret1 = clGetProgramBuildInfo(program, device_id, CL_PROGRAM_BUILD_LOG, 0, NULL, &len);
    char *buffer1 = calloc(len, sizeof(char));
    ret1 = clGetProgramBuildInfo(program, device_id, CL_PROGRAM_BUILD_LOG, len, buffer1, NULL);
    LOGE("ASDF%s",buffer1);*/

    // Create the compute kernel in the program we wish to run
    kernel = clCreateKernel(program, kernel_name, &err);
    checkCL(err);

    // Create the input and output arrays in device memory for our calculation
    d_src = clCreateBuffer(context, CL_MEM_READ_ONLY, info.width*info.height*4, NULL, &err);
    checkCL(err);

    d_dst = clCreateBuffer(context, CL_MEM_WRITE_ONLY, info.width*info.height*4, NULL, &err);
    checkCL(err);

    jsize diffculty = (*env)->GetArrayLength(env, puzzleMask);

    jint Mask[diffculty*diffculty], i, j;
    jint* tmpMask2;
    jintArray tmpMask;
    for(i=0; i<diffculty; i++) {
        tmpMask = (*env)->GetObjectArrayElement(env, puzzleMask, i);
        for (j = 0; j < diffculty; j++) {
            tmpMask2 = (*env)->GetIntArrayElements(env, tmpMask, 0);
            Mask[i * diffculty + j] = tmpMask2[j];
            (*env)->ReleaseIntArrayElements(env, tmpMask, tmpMask2, 0);
        }
    }
    LOGE("size %d", sizeof(Mask));

    cl_mem mask = clCreateBuffer(context, CL_MEM_READ_ONLY, sizeof(Mask), NULL, &err);
    checkCL(err);
    checkCL(clEnqueueWriteBuffer(queue, d_src, CL_TRUE, 0, info.width*info.height*4, tempPixels, 0, NULL, NULL));
    checkCL(clEnqueueWriteBuffer(queue, mask, CL_TRUE, 0, sizeof(Mask), &Mask, 0, NULL, NULL));

    checkCL(clSetKernelArg(kernel, 0, sizeof(cl_mem), &d_src));
    checkCL(clSetKernelArg(kernel, 1, sizeof(cl_mem), &d_dst));
    checkCL(clSetKernelArg(kernel, 2, sizeof(int), &info.width));
    checkCL(clSetKernelArg(kernel, 3, sizeof(int), &info.height));
    checkCL(clSetKernelArg(kernel, 4, sizeof(cl_mem), &mask));
    checkCL(clSetKernelArg(kernel, 5, sizeof(int), &diffculty));

    checkCL(clEnqueueNDRangeKernel(queue, kernel, 1, NULL, &globalSize, &localSize, 0, NULL, NULL));
    checkCL(clFinish(queue));
    checkCL(clEnqueueReadBuffer(queue, d_dst, CL_TRUE, 0, info.width*info.height*4, bitmapPixels, 0, NULL, NULL ));
    checkCL(clReleaseMemObject(d_src));
    checkCL(clReleaseMemObject(d_dst));
    checkCL(clReleaseProgram(program));
    checkCL(clReleaseKernel(kernel));
    checkCL(clReleaseCommandQueue(queue));
    checkCL(clReleaseContext(context));

    AndroidBitmap_unlockPixels(env, bitmap);
    free(tempPixels);

    return bitmap;
}