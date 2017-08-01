package com.example.android.imageprocessinggame;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

//TODO: consider landscape, min-width and onsavedinstance state
public class SetStartGoalActivity extends AppCompatActivity {
    private Bitmap mBitmap;
    private  int mWidth, mHeight, mPosBlank;
    private Uri mImageUri;
    private GridView mStartGridView, mGoalGridView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_start_goal);

        mWidth = getIntent().getIntExtra(getResources().getString(R.string.extra_width), 0);
        mHeight = getIntent().getIntExtra(getResources().getString(R.string.extra_height), 0);
        mPosBlank = getIntent().getIntExtra(getResources().getString(R.string.extra_blank), mWidth*mHeight-1);
        mImageUri = Uri.parse(getIntent().getStringExtra(getResources().getString(R.string.extra_uri)));
        try {
            mBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), mImageUri);
        }catch (Exception e) {
            Toast.makeText(getBaseContext(),getResources().getString(R.string.choose_other_image_error) , Toast.LENGTH_LONG).show();
            Log.d("hello","successfully detected image error");
            Intent finish = new Intent(SetStartGoalActivity.this, MainActivity.class);
            finish.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(finish);
            return;
        }
        mStartGridView = (GridView) findViewById(R.id.swap_grids_start);
        mGoalGridView = (GridView) findViewById(R.id.swap_grids_goal);
        initGridView(mStartGridView);
        initGridView(mGoalGridView);
    }

    /**
     * initialize gridView, under assumption that other instance variables are set
     * @param gridView target gridView to be init
     */
    private void initGridView(final GridView gridView) {
        gridView.setNumColumns(mWidth);
        int inputArray[] = new int [mWidth*mHeight];
        for (int i = 0 ; i < inputArray.length; i++) {
            inputArray[i] = i;
        }
        final ImageAdapter imageAdapter = new ImageAdapter(SetStartGoalActivity.this, mPosBlank, mWidth, inputArray, mBitmap);
        gridView.post(new Runnable() {
            @Override
            public void run() {
                gridView.setAdapter(imageAdapter);
            }
        });
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                int selectedPos = imageAdapter.getSelectedPosition();
                if (selectedPos < 0) {
                    imageAdapter.setSelectedPosition(position);
                } else {
                    imageAdapter.swapPositions(position, selectedPos);
                    imageAdapter.setSelectedPosition(-1);
                }
                imageAdapter.notifyDataSetChanged();
            }
        });
        gridView.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return event.getAction() == MotionEvent.ACTION_MOVE;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.game_parameter_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int menuItemSelected = item.getItemId();
        if ((menuItemSelected == R.id.next_step_menu_item)) {
            Intent gameIntent = new Intent(this, GameActivity.class);
            gameIntent.putExtra(getResources().getString(R.string.extra_width), mWidth);
            gameIntent.putExtra(getResources().getString(R.string.extra_height), mHeight);
            gameIntent.putExtra(getResources().getString(R.string.extra_uri), mImageUri.toString());
            gameIntent.putExtra(getResources().getString(R.string.extra_blank), mPosBlank);
            ImageAdapter startAdapter = (ImageAdapter)(mStartGridView.getAdapter());
            ImageAdapter goalAdapter = (ImageAdapter)(mGoalGridView.getAdapter());
            gameIntent.putExtra(getResources().getString(R.string.extra_start), startAdapter.mPositionHashes);
            gameIntent.putExtra(getResources().getString(R.string.extra_goal), goalAdapter.mPositionHashes);
            startActivity(gameIntent);
        } else if (menuItemSelected == R.id.prev_step_menu_item) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    //TODO: can move all method like this to a separate util class
    static Bitmap createBitMapWithBorder(Context context, Bitmap bmp, int borderSize) {
        Bitmap bmpWithBorder = Bitmap.createBitmap(bmp.getWidth() + borderSize * 2, bmp.getHeight() + borderSize * 2, bmp.getConfig());
        Canvas canvas = new Canvas(bmpWithBorder);
        canvas.drawColor(ContextCompat.getColor(context, R.color.colorPrimaryLight));
        canvas.drawBitmap(bmp, borderSize, borderSize, null);
        return bmpWithBorder;
    }

    static class ImageAdapter extends BaseAdapter {
        private Context mContext;
        private final int[] mPositionHashes;
        private final int mPosBlank; //constant symbol for blank, position of blank after hash
        private final int mWidth;
        private Bitmap mCachedTiles[], mCachedBlank;
        private Bitmap mCachedHighlightedTiles[], mCachedHighlightedBlank;
        private int mSelectedPosition;
        private final int mBorderSize = 10;
        private Bitmap mBitmap;

        int getSelectedPosition() {
            return mSelectedPosition;
        }


        void setSelectedPosition(int mSelectedPosition) {
            this.mSelectedPosition = mSelectedPosition;
        }

        void swapPositions(int ix1, int ix2) {
            int temp = mPositionHashes[ix1];
            mPositionHashes[ix1] =  mPositionHashes[ix2];
            mPositionHashes[ix2] = temp;
        }


        ImageAdapter(Context c, int posBlank, int width,  int [] positions, Bitmap bitmap) {
            mSelectedPosition = -1;
            mContext = c;
            mCachedTiles = new Bitmap[positions.length];
            mCachedHighlightedTiles = new Bitmap[positions.length];
            for (int i = 0; i < positions.length; i++) {
                mCachedTiles[i] = null;
                mCachedHighlightedTiles[i] = null;
            }
            mCachedBlank = null;
            mCachedHighlightedBlank = null;
            mPositionHashes = positions.clone();
            this.mBitmap = bitmap;
            mPosBlank = posBlank;
            mWidth = width;
        }

        /* indicates how many items are there in the grid view */
        public int getCount() {
            return mPositionHashes.length;
        }

        public Object getItem(int position) {
            return null;
        }


        public long getItemId(int position) {
            return 0;
        }

        // create a new ImageView for each item referenced by the Adapter
        public View getView(int position, View convertView, ViewGroup parent) {
            int image_pos = position;
            position = mPositionHashes[position];
            ImageView imageView;

            int width_ratio = mWidth;
            int height_ratio = mPositionHashes.length/mWidth;
            int idealTileWidth = (parent.getWidth()+width_ratio-1)/width_ratio, idealTileHeight = (parent.getHeight()+height_ratio-1)/height_ratio;


            if (convertView == null) {
                // if it's not recycled, initialize some attributes
                imageView = new ImageView(mContext);
                imageView.setLayoutParams(new GridView.LayoutParams(idealTileWidth, idealTileHeight));
                //imageView.setAdjustViewBounds(true);
                imageView.setScaleType(ImageView.ScaleType.CENTER);
            } else {
                imageView = (ImageView) convertView;
            }

            if (position == mPosBlank) {
                if (mCachedBlank == null || mCachedHighlightedBlank == null) {
                    Bitmap blank = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.blank);
                    mCachedBlank = Bitmap.createScaledBitmap(blank, idealTileWidth, idealTileHeight, false);
                    mCachedHighlightedBlank = Bitmap.createScaledBitmap(blank, idealTileWidth - mBorderSize * 2, idealTileHeight - mBorderSize * 2, false);
                    mCachedHighlightedBlank = createBitMapWithBorder(mContext, mCachedHighlightedBlank, mBorderSize);
                }
                imageView.setImageBitmap((image_pos==mSelectedPosition) ? mCachedHighlightedBlank : mCachedBlank);
                return imageView;
            }

            if (mCachedTiles[position] == null || mCachedHighlightedTiles[position] == null) {
                Bitmap board = mBitmap; // the puzzle that we ought to recover
                float originalImageWidth = (float) board.getWidth() / width_ratio, originalImageHeight = (float) board.getHeight() / height_ratio;
                Bitmap tile = Bitmap.createBitmap(board, (int) (originalImageWidth * (position % mWidth)), (int) (originalImageHeight * (position / mWidth)),
                        (int) originalImageWidth, (int) originalImageHeight);
                mCachedTiles[position] = Bitmap.createScaledBitmap(tile, idealTileWidth, idealTileHeight, false);
                mCachedHighlightedTiles[position] = Bitmap.createScaledBitmap(tile, idealTileWidth - mBorderSize * 2, idealTileHeight - mBorderSize * 2, false);
                mCachedHighlightedTiles[position] = createBitMapWithBorder(mContext,  mCachedHighlightedTiles[position], mBorderSize);
            }
            imageView.setImageBitmap((image_pos==mSelectedPosition) ? mCachedHighlightedTiles[position] :mCachedTiles[position]);
            return imageView;
        }
    }
}
