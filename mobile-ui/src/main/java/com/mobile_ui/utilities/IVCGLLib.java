package com.mobile_ui.utilities;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import android.content.res.Resources;
import android.opengl.GLES20;
import android.util.Log;
import android.view.OrientationEventListener;

public class IVCGLLib {

    public static final boolean DEBUG = true;
        
	public static final String TAG = "IVCGLLib";
	public static final int FEATURE_SUM = 1;
	public static final int FEATURE_AVG = 2;
	public static final int FEATURE_VAR = 3;
	public static final int FEATURE_STD = 4;
    public static final int ORIENTATION_HYSTERESIS = 5;
	public static final int FEATURE_POS_CENTER = 6;
	public static final int FEATURE_POS_DOWN = 7;
	public static final int FEATURE_POS_LEFT = 8;
	public static final int FEATURE_POS_RIGHT = 9;

	public static ByteBuffer glToByteBuffer(byte[] buffer) {
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(buffer.length);
		byteBuffer.order(ByteOrder.nativeOrder());
		byteBuffer.put(buffer);
		byteBuffer.position(0);
		return byteBuffer;
	}

	public static FloatBuffer glToFloatBuffer(float[] buffer) {
		FloatBuffer floatBuffer = null;
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(buffer.length * 4);
		byteBuffer.order(ByteOrder.nativeOrder());
		floatBuffer = byteBuffer.asFloatBuffer();
		floatBuffer.put(buffer);
		floatBuffer.position(0);
		return floatBuffer;
	}

	public static ShortBuffer glToShortBuffer(short[] buffer) {
		ShortBuffer shortBuffer = null;
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(buffer.length * 2);
		byteBuffer.order(ByteOrder.nativeOrder());
		shortBuffer = byteBuffer.asShortBuffer();
		shortBuffer.put(buffer);
		shortBuffer.position(0);
		return shortBuffer;
	}

	public static String loadFromAssetsFile(String fileName, Resources r) {
		String result = null;
		try {
			InputStream in = r.getAssets().open(fileName);
			int ch = 0;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			while ((ch = in.read()) != -1) {
				baos.write(ch);
			}
			byte[] buff = baos.toByteArray();
			baos.close();
			in.close();
			result = new String(buff, "UTF-8");
			result = result.replaceAll("\\r\\n", "\n");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public static String loadFromClasspath(String fileName) {
		String result = null;
		try {
			InputStream in = IVCGLLib.class.getResourceAsStream(fileName);
			int ch = 0;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			while ((ch = in.read()) != -1) {
				baos.write(ch);
			}
			byte[] buff = baos.toByteArray();
			baos.close();
			in.close();
			result = new String(buff, "UTF-8");
			result = result.replaceAll("\\r\\n", "\n");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public static int glCreateProgram(String vertexSource, String fragmentSource) {
		int vertexShader = glLoadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
		if (vertexShader == 0) {
			return 0;
		}

		int pixelShader = glLoadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
		if (pixelShader == 0) {
			return 0;
		}

		int program = GLES20.glCreateProgram();
		if (program != 0) {
			GLES20.glAttachShader(program, vertexShader);
			glCheckGlError("glAttachShader");
			GLES20.glAttachShader(program, pixelShader);
			glCheckGlError("glAttachShader");
			GLES20.glLinkProgram(program);
			int[] linkStatus = new int[1];
			GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
			if (linkStatus[0] != GLES20.GL_TRUE) {
				if(DEBUG) Log.d(TAG, "Could not link program: ");
				if(DEBUG) Log.d(TAG, GLES20.glGetProgramInfoLog(program));
				GLES20.glDeleteProgram(program);
				program = 0;
			}
		}
		return program;
	}

	private static int glLoadShader(int shaderType,  String source ) {
		int shader = GLES20.glCreateShader(shaderType);
		if (shader != 0) {
			GLES20.glShaderSource(shader, source);
			GLES20.glCompileShader(shader);
			int[] compiled = new int[1];
			GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
			if (compiled[0] == 0) {
				if(DEBUG) Log.d(TAG, "Could not compile shader " + shaderType + ":");
				if(DEBUG) Log.d(TAG, GLES20.glGetShaderInfoLog(shader));
				GLES20.glDeleteShader(shader);
				shader = 0;
			}
		}
		return shader;
	}

	public static void glCheckGlError(String op) {
		int error;
		while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
			if(DEBUG) Log.d(TAG, op + ": glError " + error);
			throw new RuntimeException(op + ": glError " + error);
		}
	}

	public static int glGenFBO() {
		int[] fbo = new int[1];
        IntBuffer frameBuffer = IntBuffer.wrap(fbo);
		GLES20.glGenFramebuffers(fbo.length, frameBuffer);
		return fbo[0];
	}

	public static int glGenTexImage2D(int width, int height,int interMode, byte[] matrixData) {
		int[] texs = new int[1];
        GLES20.glGenTextures(texs.length, texs, 0);	        	        
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texs[0]);
		glTexParameter(interMode);
		if (matrixData != null && matrixData.length > 0) {
			ByteBuffer MatrixBuf = ByteBuffer.wrap(matrixData);
			GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, width, height, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, MatrixBuf);
		} else {
			GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
		}
		return texs[0];
	}

    private static void glTexParameter(int interMethod) {
    	if (interMethod == 0) {
    		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
    		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
    		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
    		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);		
    	} else if (interMethod == 1) {	
    		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
    		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
    		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
    		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);	
    	}
	}

    public static void glUseFBO(int x, int y, int w, int h, boolean offScreen, int fboHandle, int textureHandle) {
    	if(!offScreen){
	    	GLES20.glViewport(x, y, w, h);
	    	GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);	
		} else{
	    	GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboHandle);
			glCheckGlError("glBindFramebuffer");
			GLES20.glViewport(x, y, w, h);
			glCheckGlError("glViewport");
			GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, textureHandle, 0);
			glCheckGlError("glFramebufferTexture2D");
			int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
			if(status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
			      throw new RuntimeException("Could not bind FBO!");
			}
		}
    }

//    public static double glGetFeature(int x, int y, int w, int h, int flag) {
//    	double feature = 0;
//    	int length = (w-x)*(h-y)*4;
//        ByteBuffer tmpBuffer = ByteBuffer.allocate(length);
//        GLES20.glReadPixels(x, y, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, tmpBuffer);
//        GLES20.glFlush();
//        byte[] rgbaBytes = tmpBuffer.array();
//        if (flag == FEATURE_SUM) {//sum
//        	feature = getRSumFromRGBA(rgbaBytes);
//        } else if (flag == FEATURE_AVG) {//avg
//        	feature = getRAvgFromRGBA(rgbaBytes);
//        } else if (flag == FEATURE_VAR) {//var
//        	feature = getRVarFromRGBA(rgbaBytes);
//        } else if (flag == FEATURE_STD) {//std
//        	feature = getRStdFromRGBA(rgbaBytes);
//        } else if (flag == FEATURE_POS_CENTER) {
//        	byte[] upParts = new byte[144];
//        	int indx, cnt = 0;
//            for (int i = 10; i < 22; i++) {
//            	for (int j = 10; j < 22; j++){
//            		indx = i * w + j;
//            		upParts[cnt] = rgbaBytes[indx*4];
//            		cnt++;
//            	}
//    		}	
//        	feature = getRPosFromRGBA(upParts, rgbaBytes);
//        } else if (flag == FEATURE_POS_DOWN) {
//        	byte[] downParts = new byte[10*w];
//        	int indx, cnt = 0;
//            for (int i = 22; i < h; i++) {
//            	for (int j = 0; j < w; j++){
//            		indx = i * w + j;
//            		downParts[cnt] = rgbaBytes[indx*4];
//            		cnt++;
//            	}
//    		}	
//        	feature = getRPosFromRGBA(downParts, rgbaBytes);
//        } else if (flag == FEATURE_POS_RIGHT) {
//        	byte[] downParts = new byte[120];
//        	int indx, cnt = 0;
//            for (int i = 22; i < h; i++) {
//            	for (int j = 0; j < 12; j++){
//            		indx = i * w + j;
//            		downParts[cnt] = rgbaBytes[indx*4];
//            		cnt++;
//            	}
//    		}	
//        	feature = getRPosFromRGBA(downParts, rgbaBytes);
//        } else if (flag == FEATURE_POS_LEFT) {
//        	byte[] downParts = new byte[120];
//        	int indx, cnt = 0;
//            for (int i = 22; i < h; i++) {
//            	for (int j = 20; j < w; j++){
//            		indx = i * w + j;
//            		downParts[cnt] = rgbaBytes[indx*4];
//            		cnt++;
//            	}
//    		}	
//        	feature = getRPosFromRGBA(downParts, rgbaBytes);
//        }    
//        return feature;
//    }
    
 //û�б任���������������
    public static double glGetFeature(int x, int y, int w, int h, int flag) {
    	double feature = 0;
    	int length = (w-x)*(h-y)*4;
    	ByteBuffer tmpBuffer = ByteBuffer.allocate(length);
    	GLES20.glReadPixels(x, y, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, tmpBuffer);
    	GLES20.glFlush();
    	byte[] rgbaBytes = tmpBuffer.array();
    	if (flag == FEATURE_SUM) {//sum
    		feature = getRSumFromRGBA(rgbaBytes);
    	} else if (flag == FEATURE_AVG) {//avg
    		feature = getRAvgFromRGBA(rgbaBytes);
    	} else if (flag == FEATURE_VAR) {//var
    		feature = getRVarFromRGBA(rgbaBytes);
    	} else if (flag == FEATURE_STD) {//std
    		feature = getRStdFromRGBA(rgbaBytes);
    	} else if (flag == FEATURE_POS_CENTER) {
    		byte[] upParts = new byte[144];
    		int indx, cnt = 0;
    		for (int i = 10; i < 22; i++) {
    			for (int j = 10; j < 22; j++){
    				indx = i * h + j;
    				upParts[cnt] = rgbaBytes[indx*4];
    				cnt++;
    			}
    		}	
    		feature = getRPosFromRGBA(upParts, rgbaBytes);
    	} else if (flag == FEATURE_POS_DOWN) {
    		byte[] downParts = new byte[10*w];
    		int indx, cnt = 0;
    		for (int i = 0; i < w; i++) {
    			for (int j = 22; j < h; j++){
    				indx = i * h + j;
    				downParts[cnt] = rgbaBytes[indx*4];
    				cnt++;
    			}
    		}	
    		feature = getRPosFromRGBA(downParts, rgbaBytes);
    	} else if (flag == FEATURE_POS_LEFT) {
    		byte[] downParts = new byte[120];
    		int indx, cnt = 0;
    		for (int i = 0; i < 12; i++) {
    			for (int j = 22; j < h; j++){
    				indx = i * h + j;
    				downParts[cnt] = rgbaBytes[indx*4];
    				cnt++;
    			}
    		}	
    		feature = getRPosFromRGBA(downParts, rgbaBytes);
    	} else if (flag == FEATURE_POS_RIGHT) {
    		byte[] downParts = new byte[120];
    		int indx, cnt = 0;
    		for (int i = 20; i < w; i++) {
    			for (int j = 22; j < h; j++){
    				indx = i * h + j;
    				downParts[cnt] = rgbaBytes[indx*4];
    				cnt++;
    			}
    		}	
    		feature = getRPosFromRGBA(downParts, rgbaBytes);
    	}    
    	return feature;
    }
    
    private static double getRPosFromRGBA(byte[] img, byte[] rgba) {
        int rLength = img.length;
    	double avg = getRAvgFromRGBA(rgba);
//		long sum = 0;
//        for (int i = 0; i < rLength; i++) {
//			sum += (int)(img[i]&0xff);
//		}
//        avg = sum/rLength;
		
		double denom = 0, numer = 0;
		for (int i = 0; i < rLength; i++) {
			denom += Math.pow((img[i]&0xff) - avg, 2);
			numer += Math.pow((img[i]&0xff) - avg, 3);
		}
		denom = Math.pow(Math.sqrt(denom / rLength), 3);
		double s;
		if (denom != 0) {
			numer = numer / rLength;
			s = numer/denom;
		} else {
			s = 1000;
		}
		return s;
	}
//    public static double glGetFeature(int x, int y, int w, int h, int flag) {
//    	double feature = 0;
//    	int length = (w-x)*(h-y)*4;
//        ByteBuffer tmpBuffer = ByteBuffer.allocate(length);
//        GLES20.glReadPixels(x, y, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, tmpBuffer);
//        GLES20.glFlush();
//        byte[] rgbaBytes = tmpBuffer.array();
//        if (flag == FEATURE_SUM) {//sum
//        	feature = getRSumFromRGBA(rgbaBytes);
//        } else if (flag == FEATURE_AVG) {//avg
//        	feature = getRAvgFromRGBA(rgbaBytes);
//        } else if (flag == FEATURE_VAR) {//var
//        	feature = getRVarFromRGBA(rgbaBytes);
//        } else if (flag == FEATURE_STD) {//std
//        	feature = getRStdFromRGBA(rgbaBytes);
//        }
//        return feature;
//    }
    /**
     * �������
     * @param rgba ����Դ
     * @return ��ͽ��
     */
	private static long getRSumFromRGBA(byte[] rgba) {
        long sum = 0;
        int rLength = rgba.length/4;
        for (int i = 0; i < rLength; i++) {
			sum += (rgba[i*4]&0xff);
		}
        return sum;
    }
	/**
	 * �������ֵ
	 * @param rgba ����Դ
	 * @return ��ֵ
	 */
	private static double getRAvgFromRGBA(byte[] rgba) {
        double sum = getRSumFromRGBA(rgba);
        return sum/(rgba.length/4);
    }
	/**
	 * �����󷽲�
	 * @param rgba ����Դ
	 * @return ����
	 */
    private static double getRVarFromRGBA(byte[] rgba) {
    	double var = 0;
        int rLength = rgba.length/4;
        double avg = getRAvgFromRGBA(rgba);
        for (int i = 0; i < rLength; i++) {
        	var += Math.pow((rgba[i*4]&0xff) - avg, 2);	        	
		}
		return var;
	}

    private static double getRStdFromRGBA(byte[] rgba) {
		double var = getRVarFromRGBA(rgba);
		double std = Math.sqrt(var/(rgba.length/4));
		return std;
	}

    public static void glGetHistogram(int x, int y, int w, int h, int[] resultR, int[] resultG, int[] resultB) {
    	int length = (w-x)*(h-y)*4;        
        ByteBuffer tmpBuffer = ByteBuffer.allocate(length);
        GLES20.glReadPixels(x, y, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, tmpBuffer);
        GLES20.glFlush();

        byte[] rgbaBytes = tmpBuffer.array();
		int bin = 0;
        for (int i = 0; i < length; i++) {
			if(i%4 == 0) {					
				resultR[bin] += rgbaBytes[i] & 0xFF;					
				resultG[bin] += rgbaBytes[i+1] & 0xFF;					
				resultB[bin] += rgbaBytes[i+2] & 0xFF;
				bin++;
				bin %= 256;
			}
		}
    }

    public static int getNumInHistArray(int[] hist, int thresLow, int thresHigh) {
    	int num = 0;
    	int low = 0, high = 0;
    	if (thresLow == -1) {
    		low = thresHigh-1;
    		high = 256;
    	} else if (thresHigh == -1) {
    		low = 0;
    		high = thresLow+1;
    	} else {
    		low = thresLow-1;
    		high = thresHigh+1;
    	}
    	for (int i = low; i < high; i++) {
			num += hist[i];
		}
    	
    	return num;
    }

	public static float[] getSplitPointData(int a) {
		int length = a * a;
		float data[] =  new float[length*2];
		float tmp = 1.0f/(a-1);			
		for (int i = 0; i < length; i++) {
			data[2*i] = (i/a)*tmp;
			data[2*i+1] = (i%a)*tmp;
		}
		return data;
	}

    public static int roundOrientation(int orientation, int orientationHistory) {
        boolean changeOrientation = false;
        if (orientationHistory == OrientationEventListener.ORIENTATION_UNKNOWN) {
            changeOrientation = true;
        } else {
            int dist = Math.abs(orientation - orientationHistory);
            dist = Math.min( dist, 360 - dist );
            changeOrientation = ( dist >= 45 + ORIENTATION_HYSTERESIS );
        }
        if (changeOrientation) {
            return ((orientation + 45) / 90 * 90) % 360;
        }
        return orientationHistory;
    }
}
