package parlab.example.slidepuzzle;

import android.util.Log;

public class puzzleFunc {
    public static void initArray(int[][] arr, int NUM){
        int i, j;
        for(j=0; j<NUM; j++){
            for(i=0; i<NUM; i++){
                arr[j][i]=j*NUM+i+1;
            }
        }
    }
    public static void logArray(int[][] arr, int NUM){
        String stree="array\n";
        for(int ii=0; ii<NUM; ii++){
            for(int jj=0; jj<NUM; jj++){
                if(arr[ii][jj]<10)stree+=" ";
                stree+=String.valueOf(arr[ii][jj]);
                stree+=" ";
            }
            stree+="\n";
        }
        Log.d("test", stree);
    }

    public static void randomArray(int[][] arr, int[] witch, int NUM){
        int i, j, tmpValue;
        int[] tmp1 = new int[2];
        int[] tmp2 = new int[2];
        int random= (int) (Math.random()*NUM*100);
        for(i=0; i<random; i++){
            tmp1[0]= (int) (Math.random()*NUM);
            tmp1[1]= (int) (Math.random()*NUM);
            tmp2[0]= (int) (Math.random()*NUM);
            tmp2[1]= (int) (Math.random()*NUM);
            tmpValue=arr[tmp1[1]][tmp1[0]];
            arr[tmp1[1]][tmp1[0]]=arr[tmp2[1]][tmp2[0]];;
            arr[tmp2[1]][tmp2[0]]=tmpValue;
        }

        for(j=0; j<NUM; j++){
            for(i=0; i<NUM; i++){
                if(arr[j][i]==0){witch[0]=i; witch[1]=j; break;}
            }
        }
    }
    public static void moveLeft(int[][] arr, int[] witch, int NUM){
        if(witch[0]==0) return;
        arr[witch[1]][witch[0]]=arr[witch[1]][witch[0]-1];
        arr[witch[1]][witch[0]-1]=0;
        witch[0]--;
    }
    public static void moveRight(int[][] arr, int[] witch, int NUM){
        if(witch[0]==NUM-1) return;
        arr[witch[1]][witch[0]]=arr[witch[1]][witch[0]+1];
        arr[witch[1]][witch[0]+1]=0;
        witch[0]++;
    }
    public static void moveUp(int[][] arr, int[] witch, int NUM){
        if(witch[1]==0) return;
        arr[witch[1]][witch[0]]=arr[witch[1]-1][witch[0]];
        arr[witch[1]-1][witch[0]]=0;
        witch[1]--;
    }
    public static void moveDown(int[][] arr, int[] witch, int NUM){
        if(witch[1]==NUM-1) return;
        arr[witch[1]][witch[0]]=arr[witch[1]+1][witch[0]];
        arr[witch[1]+1][witch[0]]=0;
        witch[1]++;
    }
    public static boolean isSolvable(int[][] arr, int witchY, int NUM){
        int[] grid= new int[NUM*NUM];
        int i, j;
        int result=0;
        for(j=0; j<NUM; j++){
            for(i=0; i<NUM; i++){
                grid[j*NUM+i]=arr[j][i];
                if(grid[j*NUM+i]==0) grid[j*NUM+i]=16;
            }
        }

        for(i=0; i<NUM*NUM; i++){
            if(grid[i]==16) continue;
            for(j=i+1; j<NUM*NUM; j++){
                if(grid[i]>grid[j]) result++;
            }
        }

        return ((NUM%2==1) && (result%2==0))||((NUM%2==0)&&((result%2==0)&&((NUM-witchY)%2==1))||((result%2==1)&&((NUM-witchY)%2==0)));
    }
    public static boolean isMatched(int[][] arr, int NUM){
        int i, j;
        for(j=0; j<NUM; j++){
            for(i=0; i<NUM; i++){
                if(i==NUM-1 && j==NUM-1) return true;
                if(arr[j][i]!=j*NUM+i+1) return false;
            }
        }
        return false;
    }
}
