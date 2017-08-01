package com.example.android.imageprocessinggame;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;

public class GameParameterCustomized extends GenericGameParameter {
    private  boolean mBlankTargeted;
    private PosBlankAdapter mGridViewAdapter;

    @Override
    Class<?> classToJumpTo() {
        return SetStartGoalActivity.class;
    }

    @Override
    boolean widthHeightSetBeforeImageSelection() {
        return true;
    }

    @Override
    void initGridView(final GridView gridView) {
        gridView.post(new Runnable() {
            @Override
            public void run() {
                gridView.setNumColumns(1);
                gridView.setAdapter(new SingleImageAdapter(GameParameterCustomized.this,
                        BitmapFactory.decodeResource(getResources(), R.drawable.not_yet_selected)));
            }
        });
    }

    @Override
    boolean isCorrectAtFirst(GridView gridView) {
        return false;
    }

    @Override
    int getImageIdExtra(GridView gridView) {
        return 0; //anything
    }

    @Override
    int getPosBlankExtra(GridView gridView) {
        PosBlankAdapter adapter = (PosBlankAdapter)(gridView.getAdapter());
        return adapter.getPosBlank();
    }

    @Override
    void afterImageSelected(final GridView gridView, final Bitmap bitmap, final Button button, final int width, final int height) {
        button.setText(getResources().getString(R.string.choose_blank));
        mGridViewAdapter = new PosBlankAdapter(GameParameterCustomized.this, width, height, bitmap);
        gridView.post(new Runnable() {
            @Override
            public void run() {
                gridView.setNumColumns(width);
                gridView.setAdapter(mGridViewAdapter);
            }
        });

        button.setOnClickListener(null);
        mBlankTargeted = false;
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //PosBlankAdapter adapter = (PosBlankAdapter) gridView.getAdapter();
                if (!mBlankTargeted) {
                    mBlankTargeted = true;
                    button.setText(getResources().getString(R.string.blank_chosen));
                    mGridViewAdapter = new PosBlankAdapter(GameParameterCustomized.this, width, height, bitmap);
                    gridView.post(new Runnable() {
                        @Override
                        public void run() {
                            gridView.setAdapter(mGridViewAdapter);
                       }
                    });
                }
                mGridViewAdapter.setPosBlank(position);
                mGridViewAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    boolean isCorrectAfter(GridView gridView) {
        PosBlankAdapter adapter = (PosBlankAdapter)(gridView.getAdapter());
        return adapter.getPosBlank() > -1;
    }


    private class PosBlankAdapter extends BaseAdapter {
        private Context mContext;
        private int mPosBlank;
        private final int mWidth;
        private final int mHeight;
        private final Bitmap[] mCacheBitmaps;
        private Bitmap mCacheBlank;
        private final Bitmap mBitmap;


        int getPosBlank() {
            return mPosBlank;
        }

        void setPosBlank(int mPosBlank) {
            this.mPosBlank = mPosBlank;
        }



        PosBlankAdapter(Context c, int width, int height, Bitmap bitmap) {
            mContext = c;
            mPosBlank = -1;
            mCacheBitmaps = new Bitmap[width * height];
            for (int i = 0; i < width * height; i++) {
                mCacheBitmaps[i] = null;
            }
            mCacheBlank = null;
            mBitmap = bitmap;
            mWidth = width;
            mHeight = height;
        }



        /* indicates how many items are there in the grid view */
        public int getCount() {
            return mWidth*mHeight;
        }

        public Object getItem(int position) {
            return null;
        }


        public long getItemId(int position) {
            return 0;
        }

        // create a new ImageView for each item referenced by the Adapter
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;

            int width_ratio = mWidth;
            int height_ratio = mHeight;
            int idealTileWidth = parent.getWidth() / width_ratio, idealTileHeight = (parent.getHeight()+height_ratio-1) / height_ratio;

            if (convertView == null) {
                imageView = new ImageView(mContext);
                imageView.setLayoutParams(new GridView.LayoutParams(idealTileWidth, idealTileHeight));
                //imageView.setAdjustViewBounds(true);
                imageView.setScaleType(ImageView.ScaleType.CENTER);
            } else {
                imageView = (ImageView) convertView;
            }

            if (position == mPosBlank) {
                if (mCacheBlank == null) {
                    Bitmap blank = BitmapFactory.decodeResource(getResources(), R.drawable.blank);
                    mCacheBlank = Bitmap.createScaledBitmap(blank, idealTileWidth, idealTileHeight, false);
                }
                imageView.setImageBitmap(mCacheBlank);
                return imageView;
            }

            if (mCacheBitmaps[position] == null) {
                Bitmap board = mBitmap;
                float originalImageWidth = (float) board.getWidth() / width_ratio, originalImageHeight = (float) board.getHeight() / height_ratio;
                Bitmap tile = Bitmap.createBitmap(board, (int) (originalImageWidth * (position % mWidth)), (int) (originalImageHeight * (position / mWidth)),
                        (int) originalImageWidth, (int) originalImageHeight);
                mCacheBitmaps[position] = Bitmap.createScaledBitmap(tile, idealTileWidth, idealTileHeight, false);
            }
            imageView.setImageBitmap(mCacheBitmaps[position]);
            return imageView;
        }
    }
}


