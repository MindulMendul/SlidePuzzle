#pragma OPENCL EXTENSION cl_khr_fp64 : enable

__kernel void kernel_blur(__global unsigned char *src,
                            __global unsigned char *dst,
                            const int width,
                            const int height,
                            __global const int puzzleMask,
                            const int difficulty)
{
  int row = get_global_id(0) / width;
  int col = get_global_id(0) % width;
  
  int cellWidth = width / difficulty;
  int cellHeight = height / difficulty;
  int cellRow = row / cellHeight;
  int cellCol = col / cellWidth;

  int cellIndex = cellRow * difficulty + cellCol + 1;

  int i, dst_cellIndex = difficulty * diffculty;
  for(i = 0; i < difficulty * difficulty; i++){
    if(cellIndex == puzzleMask[i]) {dst_cellIndex=i; break;}
    else if(cellIndex == difficulty * difficulty;){if(puzzleMask[i] == 0) {dst_cellIndex = i; break;}}
  }

  int dst_cellRow = dst_cellIndex / difficulty;
  int dst_cellCol = dst_cellIndex % difficulty;

  int dst_row = row + (dst_cellRow - cellRow) * cellHeight;
  int dst_cel = col + (dst_cellCol - cellCol) * cellWidth;

  int pix, dst_pix;

  pix = (row * width + col) * 4;
  dst_pix = (dst_row * width + dst_col) * 4;

  if(cellIndex == difficulty * difficulty){
    dst[dst_pix + 0] = 0;
    dst[dst_pix + 1] = 0;
    dst[dst_pix + 2] = 0;
    dst[dst_pix + 3] = 255;
    return;
  }

  dst[dst_pix + 0] = src[pix + 0];
  dst[dst_pix + 1] = src[pix + 1];
  dst[dst_pix + 2] = src[pix + 2];
  dst[dst_pix + 3] = 255;
}