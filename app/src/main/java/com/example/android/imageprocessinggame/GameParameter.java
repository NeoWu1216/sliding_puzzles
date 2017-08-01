package com.example.android.imageprocessinggame;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;

import java.util.Arrays;

public class GameParameter extends GenericGameParameter{
    private static int[] mImages = new int [] {R.drawable.doraemon, R.drawable.sample_8, R.drawable.captain_american, R.drawable.car};
    private ImageAdapter mGridViewAdapter = null;

    @Override
    Class<?> classToJumpTo() {
        return GameActivity.class;
    }

    @Override
    boolean widthHeightSetBeforeImageSelection() {
        return false;
    }

    @Override
    void initGridView(final GridView gridView) {
        gridView.post(new Runnable() {
            @Override
            public void run() {
                mGridViewAdapter = new ImageAdapter(GameParameter.this);
                gridView.setAdapter(mGridViewAdapter);
            }
        });
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                if (mGridViewAdapter != null) {
                    mGridViewAdapter.setPosSelected(position);
                    mGridViewAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    boolean isCorrectAtFirst(final GridView gridView) {
        return mGridViewAdapter.getPosSelected()>-1;
    }

    @Override
    int getImageIdExtra(GridView gridView) {
        return mImages[mGridViewAdapter.getPosSelected()];
    }

    @Override
    int getPosBlankExtra(GridView gridView) {
        return -1;
    }

    @Override
    void afterImageSelected(final GridView gridView,  final Bitmap bitmap, Button button, int width, int height) {
        gridView.post(new Runnable() {
            @Override
            public void run() {
                gridView.setNumColumns(1);
                gridView.setAdapter(new SingleImageAdapter(GameParameter.this, bitmap));
                gridView.setOnItemClickListener(null);
            }
        });
        button.setText(getResources().getString(R.string.other_image_chosen));
    }

    @Override
    boolean isCorrectAfter(final GridView gridView) {
        return true;
    }


    private  class CreatingScaledImages extends AsyncTask<Void, Void, Bitmap []> {
        @Override
        protected Bitmap[] doInBackground(Void... params) {
            Log.d("hello", "async starts");
            int num_images = mImages.length;
            int borderSize = 15;
            Bitmap imageBitmaps[] = new Bitmap[2*num_images];

            for (int position = 0; position < num_images; position++) {
                Bitmap tile = BitmapFactory.decodeResource(getResources(), mImages[position]); // the puzzle that we ought to recover (sample 8 too big)
                //rescale since device width flexible
                imageBitmaps[position] = Bitmap.createScaledBitmap(tile, mGridViewAdapter.idealTileWidth, mGridViewAdapter.idealTileHeight, false);
                imageBitmaps[position+num_images] = Bitmap.createScaledBitmap(tile,
                        mGridViewAdapter.idealTileWidth - borderSize * 2, mGridViewAdapter.idealTileHeight - borderSize * 2, false);
                imageBitmaps[position+num_images] = SetStartGoalActivity.createBitMapWithBorder(GameParameter.this,
                        imageBitmaps[position+num_images], borderSize);
            }

            return imageBitmaps;
        }

        @Override
        protected void onPostExecute(Bitmap[] params) {
            super.onPostExecute(params);
            if (mGridViewAdapter != null) {
                mGridViewAdapter.mOrigBitmaps = Arrays.copyOfRange(params, 0, params.length/2);
                mGridViewAdapter.mHighlightedBitmaps = Arrays.copyOfRange(params, params.length/2, params.length);
                mGridViewAdapter.notifyDataSetChanged();
            }
        }
    }



    private class ImageAdapter extends BaseAdapter {
        private Context mContext;
        private int posSelected = -1;
        private Bitmap[] mOrigBitmaps, mHighlightedBitmaps = null;
        void setPosSelected(int position) {
            posSelected = position;
        }
        int getPosSelected() {
            return posSelected;
        }
        private AsyncTask mTask = null;
        private int idealTileWidth, idealTileHeight = -1;

        ImageAdapter(Context c) {
            mContext = c;
        }

        /* indicates how many items are there in the grid view, so that getView will be called from position 0 to getCount()-1 */
        public int getCount() {
            return mImages.length;
        }

        public Object getItem(int position) {
            return null;
        }


        public long getItemId(int position) {
            return 0;
        }

        // create a new ImageView for each item referenced by the Adapter
        public View getView(int position, View convertView, ViewGroup parent) {
            if (idealTileHeight == -1) {
                int width_ratio = 2;
                int height_ratio = (getCount() + width_ratio - 1) / width_ratio;
                idealTileWidth = parent.getWidth() / width_ratio;
                idealTileHeight = (parent.getHeight()+height_ratio-1) / height_ratio;
            }
            ImageView imageView;

            if (convertView == null) {
                // if it's not recycled, initialize some attributes
                imageView = new ImageView(mContext);
                imageView.setLayoutParams(new GridView.LayoutParams(idealTileWidth, idealTileHeight));
                //imageView.setAdjustViewBounds(true);
                imageView.setScaleType(ImageView.ScaleType.CENTER);
            } else {
                imageView = (ImageView) convertView;
            }

            if (mHighlightedBitmaps == null) {
                if (mTask == null) {
                    mTask = new CreatingScaledImages().execute();
                }
                return imageView;
            }

            imageView.setImageBitmap((position==posSelected) ? mHighlightedBitmaps[position] : mOrigBitmaps[position]);
            return imageView;
        }
    }


}
