package com.example.android.imageprocessinggame;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
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
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;



public class GameActivity extends AppCompatActivity {
    private Puzzle mPuzzle;
    private GridView mGridView, mGoalGridView;
    private SetStartGoalActivity.ImageAdapter mGoalGridAdapter;
    private boolean mGoalGridShown = true;
    private boolean mAutoSolverDone = false;
    private String mButtonText, mButtonTextWithLine;
    private int mSize;
    private Button mSolveButton;
    private ProgressDialog mProgressDialog;

    private int[] blanks; //record blank position for move backward
    private int[] origPos;

    private boolean alreadySolved; //Used to avoid  repeated trying to get success
    private int moveIx;

    private int mImageId;
    private Bitmap mBitmap = null;
    private AsyncTask mTask = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_acitivity);

        mButtonText = getResources().getString(R.string.auto_solve);
        mButtonTextWithLine = getResources().getString(R.string.auto_solve_2_lines);

        final int width = getIntent().getIntExtra(getResources().getString(R.string.extra_width), 0);
        final int height = getIntent().getIntExtra(getResources().getString(R.string.extra_height), 0);
        int posBlank = getIntent().getIntExtra(getResources().getString(R.string.extra_blank), width*height-1); //original position of blank (i.e. symbol)

        mImageId = getIntent().getIntExtra(getResources().getString(R.string.extra_id), 0);// 0 for null
        if (mImageId == 0) {
            Uri imageUri = Uri.parse(getIntent().getStringExtra(getResources().getString(R.string.extra_uri)));
            try {
                mBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            }catch (Exception e) {
                Toast.makeText(getBaseContext(),getResources().getString(R.string.choose_other_image_error) , Toast.LENGTH_LONG).show();
                Log.d("hello","successfully detected image error");
                Intent finish = new Intent(GameActivity.this, MainActivity.class);
                finish.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(finish);
                return;
            }
        }

        if ((mImageId == 0) == (mBitmap == null)) {
            Log.d("hello","bad things happened");
            Toast.makeText(getBaseContext(),"Something terrible has happened in game image initialization" , Toast.LENGTH_LONG).show();
            Intent finish = new Intent(GameActivity.this, MainActivity.class);
            finish.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(finish);
            return;
        }

        if (!GameParameter.ValidateInput(Integer.toString(width)) || !GameParameter.ValidateInput(Integer.toString(height)) ||
                (posBlank < 0) || (posBlank >= width*height)) {
            Toast.makeText(this, "Sorry, parameters are not set correctly. Please retry or report this error.", Toast.LENGTH_LONG).show();
            finish();
        }



        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setMessage(getResources().getString(R.string.progress_dialog_message));
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setProgress(0);
        mProgressDialog.setMax(100);


        alreadySolved = false;

        origPos = new int[width*height]; //must be at least 4
        for (int i = 0; i < origPos.length; i++) {
            origPos[i] = i;
        }

        int[] startPosHashes = getIntent().getIntArrayExtra(getResources().getString(R.string.extra_start));
        int[] goalPosHashes = getIntent().getIntArrayExtra(getResources().getString(R.string.extra_goal));
        if ((startPosHashes!=null) && (goalPosHashes!=null)) {
            int[] inverseGoalPos = goalPosHashes.clone();
            for (int i = 0; i < goalPosHashes.length; i++) {
                inverseGoalPos[goalPosHashes[i]] = i;
            }
            int currBlank = 0;
            for (int i = 0; i < inverseGoalPos.length; i++) {
                origPos[i] = inverseGoalPos[startPosHashes[i]];
                if (startPosHashes[i]==posBlank) {
                    currBlank = i;
                }
            }
            try {
                mPuzzle = new Puzzle(origPos, currBlank, width);
            } catch (IllegalArgumentException e) {
                Toast.makeText(GameActivity.this, getResources().getString(R.string.puzzle_unsolvable_error), Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            if (mPuzzle.getCurrentStatus() == 1 && !alreadySolved) {
                Toast.makeText(GameActivity.this, getResources().getString(R.string.game_win) , Toast.LENGTH_LONG).show();
                alreadySolved = true;
            }
            origPos = startPosHashes;
        } else {
            goalPosHashes = origPos.clone();
            int curBlank;
            boolean isSolved = true;
            while (isSolved) {
                curBlank = randomShuffle(origPos, posBlank);

                try {
                    mPuzzle = new Puzzle(origPos, curBlank, width);
                } catch (IllegalArgumentException e) {

                    if (curBlank < 2) {
                        int temp = origPos[origPos.length - 1];
                        origPos[origPos.length - 1] = origPos[origPos.length - 2];
                        origPos[origPos.length - 2] = temp;
                    } else {
                        int temp = origPos[0];
                        origPos[0] = origPos[1];
                        origPos[1] = temp;
                    }
                    mPuzzle = new Puzzle(origPos, curBlank, width);
                }

                isSolved = (mPuzzle.getCurrentStatus() == 1);
            }
        }


        mSize = origPos.length;
        moveIx = 0;


        mGridView = (GridView) findViewById(R.id.game_table);
        mGridView.setNumColumns(width);

        LinearLayout linearLayout = (LinearLayout)findViewById(R.id.bottomLayout);
        mGoalGridView = (GridView) linearLayout.findViewById(R.id.game_display_table);
        mGoalGridView.setNumColumns(width);

        mSolveButton = (Button) linearLayout.findViewById(R.id.solve_button);
        setButton(mSolveButton, mGoalGridShown);


        if (mBitmap == null) {
            mBitmap = BitmapFactory.decodeResource(getResources(), mImageId);
        }

        final int posBlankFinal = posBlank;
        mGridView.post(new Runnable() {
            @Override
            public void run() {
                mGridView.setAdapter(new ImageAdapter(GameActivity.this, origPos, posBlankFinal, width));
            }
        });

        final int[] goalPosHashesFinal = goalPosHashes;

        mGoalGridAdapter = new SetStartGoalActivity.ImageAdapter(GameActivity.this, posBlank, width, goalPosHashesFinal, mBitmap);
        mGoalGridView.post(new Runnable() {
            @Override
            public void run() {
                mGoalGridView.setAdapter(mGoalGridAdapter);
            }
        });

        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                moveFrom(position);
            }
        });


        mGridView.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return event.getAction() == MotionEvent.ACTION_MOVE;
            }
        });



        mSolveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPuzzle.getCurrentStatus() != 1) {
                    mSolveButton.setVisibility(View.INVISIBLE);
                    mSolveButton.setOnClickListener(null);
                    mSolveButton.setClickable(false);
                    mGridView.setOnItemClickListener(null);
                    mTask =  new AutoSolveTask().execute(mPuzzle);
                }
            }
        });
    }




    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mTask != null) {
            mTask.cancel(true);
        }
    }

    private void setMove(int ix, int total_length) {
        String moveString = getResources().getString(R.string.moves_left)+Integer.toString(total_length-ix);
        mSolveButton.setText(moveString);
    }

    /**
     * swaps views by adapter, get real positions from puzzle
     * @param position real coordinate position of image the user requested to move
     */
    private void moveFrom (int position) {
        int blank_pos = mPuzzle.getPosBlank();

        if (mPuzzle.move(position)) {
            ImageAdapter adapter = (ImageAdapter)mGridView.getAdapter();
            int val = adapter.mPositionHashes[position];
            adapter.mPositionHashes[position] = adapter.mPositionHashes[blank_pos];
            adapter.mPositionHashes[blank_pos] = val;

            adapter.notifyDataSetChanged();
            mGridView.invalidateViews();
            if (mPuzzle.getCurrentStatus() == 1 && !alreadySolved) {
                Toast.makeText(GameActivity.this, getResources().getString(R.string.game_win) , Toast.LENGTH_LONG).show();
                alreadySolved = true;
            }
        }
    }

    private int randomShuffle(int[] array, int observe) {
        Random rand = new Random();
        for (int i = 0; i < array.length; i++) {
            int value = rand.nextInt(array.length);

            int temp = array[i];
            array[i] = array[value];
            array[value] = temp;
            if (i == observe) {
                observe = value;
            } else if (value == observe) {
                observe = i;
            }
        }
        return observe;
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    private class AutoSolveTask extends AsyncTask<Puzzle, Integer, int[]> {
        private int times = 0;
        private final int totalTimes = 100;

        @Override
        protected void onPreExecute() {
            mProgressDialog.show();
        }

        protected int[] doInBackground(Puzzle... puzzles) {
            //estimate based on time
            Puzzle puzzle = puzzles[0];
            int size = puzzle.getBoardCopy().length;
            long estimatedTime = 150000;
            if (size <= 12) {
                estimatedTime = 4500;
            }else if (size <= 16) {
                estimatedTime = 15000;
            } else if (size <= 20) {
                estimatedTime = 30000;
            }


            final Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                public void run() {
                    if (isCancelled()) {
                        Log.d("hello", "Zombie go away");
                        android.os.Process.killProcess(android.os.Process.myPid());
                    }
                    if (times < totalTimes) {
                        publishProgress(0);
                    }
                }
            }, estimatedTime/totalTimes, estimatedTime/totalTimes);
            int [] moves;
            try {
                moves = (size < 10) ? AutoPuzzleSolver.autoSolve(puzzle) : AutoPuzzleSolver.pieceWiseAutoSolve(puzzle);
            } catch (IllegalStateException e) {
                moves = null;
            }
            if (moves != null) {
                Log.d("hello", Integer.toString(moves.length));
            }
            timer.cancel();
            return moves;
        }

        protected void onProgressUpdate(Integer... progress) {
            times ++;
            if (times < totalTimes) {
                mProgressDialog.setProgress((int)(times*(100./totalTimes)));
            }
        }

        protected void onPostExecute(final int[] moves) {
            mProgressDialog.setProgress(100);
            mProgressDialog.cancel();

            mGoalGridAdapter = null;
            mGoalGridView.setAdapter(null);
            mAutoSolverDone = true;
            setButton(mSolveButton, false);
            if (moves == null) {
                mSolveButton.setVisibility(View.VISIBLE);
                mSolveButton.setText(getResources().getString(R.string.out_of_memory_error));
                return;
            }


            blanks = moves.clone();
            setMove(0, blanks.length);
            mSolveButton.setVisibility(View.VISIBLE);
            mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View v,
                                        int position, long id) {
                    if (moveIx != moves.length) {
                        blanks[moveIx] = mPuzzle.getPosBlank();
                        position = moves[moveIx++];
                        moveFrom(position);
                        setMove(moveIx, blanks.length);
                    }
                }
            });
            LinearLayout layout = (LinearLayout) findViewById(R.id.game_panel);
            layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (moveIx != 0) {
                        moveFrom(blanks[--moveIx]);
                        setMove(moveIx, blanks.length);
                    }
                }
            });
        }
    }

    //TODO: move all adapters into one generic class (right now too many repetitions accomplish the same functionality)
    /**
     * class ImageAdapter
     * show the puzzle from viewer's perspective
     * Quiet different from puzzle (which is from goal's perspective)
     * One notable difference is posBlank for ImageAdapter is fixed as symbol, but not for puzzle
     */
    private class ImageAdapter extends BaseAdapter {
        private Context mContext;
        private final int[] mPositionHashes;
        private final int mPosBlank; //constant symbol for blank, position of blank after hash
        private final int mWidth;
        private Bitmap mCachedTiles[], mCachedBlank;

        ImageAdapter(Context c, int[] positions, int posBlank, int width) {
            mContext = c;
            int [] copy = positions.clone();
            Arrays.sort(copy);
            mCachedTiles = new Bitmap[positions.length];
            for (int i = 0; i < copy.length; i++) {
                if (copy[i] != i) {
                    throw new IllegalArgumentException("Input should be puzzle convention");
                }
                mCachedTiles[i] = null;
            }
            mCachedBlank = null;
            mPositionHashes = positions.clone();
            mPosBlank = posBlank;
            mWidth = width;
        }

        /* indicates how many items are there in the grid view */
        public int getCount() {
            return mSize;
        }

        public Object getItem(int position) {
            return null;
        }


        public long getItemId(int position) {
            return 0;
        }

        // create a new ImageView for each item referenced by the Adapter
        public View getView(int position, View convertView, ViewGroup parent) {
            //position here is the position of tile
            position = mPositionHashes[position];
            //position now becomes the symbol of the tile
            ImageView imageView;

            int width_ratio = mWidth;
            int height_ratio = mPositionHashes.length/mWidth;
            int idealTileWidth = parent.getWidth()/width_ratio, idealTileHeight = parent.getHeight()/height_ratio;


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
                if (mCachedBlank == null) {
                    Bitmap blank = BitmapFactory.decodeResource(getResources(), R.drawable.blank);
                    mCachedBlank = Bitmap.createScaledBitmap(blank, idealTileWidth, idealTileHeight, false);
                }
                imageView.setImageBitmap(mCachedBlank);
                return imageView;
            }

            if (mCachedTiles[position] == null) {
                Bitmap board = (mBitmap == null) ? BitmapFactory.decodeResource(getResources(), mImageId) : mBitmap; // the puzzle that we ought to recover
                float originalImageWidth = (float) board.getWidth() / width_ratio, originalImageHeight = (float) board.getHeight() / height_ratio;
                Bitmap tile = Bitmap.createBitmap(board, (int) (originalImageWidth * (position % mWidth)), (int) (originalImageHeight * (position / mWidth)),
                        (int) originalImageWidth, (int) originalImageHeight);
                mCachedTiles[position] = Bitmap.createScaledBitmap(tile, idealTileWidth, idealTileHeight, false);
            }
            imageView.setImageBitmap(mCachedTiles[position]);
            return imageView;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.game_activity_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int menuItemSelected = item.getItemId();
        if (menuItemSelected == R.id.go_back_to_main_menu_item) {
            Intent finish = new Intent(GameActivity.this, MainActivity.class);
            finish.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(finish);
        } else if (menuItemSelected == R.id.show_hide_goal_menu_item) {
            if (!mAutoSolverDone) {
                if (mGoalGridShown) {
                    mGoalGridShown = false; //Must ber set first
                    mGoalGridView.setVisibility(View.GONE);
                    setButton(mSolveButton, false);
                } else { // not yet shown
                    mGoalGridShown = true;
                    setButton(mSolveButton, true);
                    mGoalGridView.setVisibility(View.VISIBLE);
                }
            }
        }
        return super.onOptionsItemSelected(item);
    }


    private void setButton(Button button, boolean showGrid) {
        //Should be consistent with layout
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)button.getLayoutParams();
        params.width = showGrid ? LinearLayout.LayoutParams.WRAP_CONTENT : LinearLayout.LayoutParams.MATCH_PARENT;
        button.setLayoutParams(params);
        button.setText(showGrid ? mButtonTextWithLine : mButtonText);
    }
}

