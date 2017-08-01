package com.example.android.imageprocessinggame;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;


/**
 * A generic class for game parameter activity
 * With editText in current implementation
 * The layout composed of 2 editText, 1 button and  1 gridView (plus menu item)
 * Since editText is for width and height, its code logic is fixed
 * At first, you can initialize gridView in any way, thus validation and passing variable is flexible, but button is fixed for select image
 * after image is selected you can recreate gridView in any way, so is validation, though this time button can change.
 * Sometimes, width and height must be fixed before image can be selected and after image selection editTexts can't be changed
 * You can safely assume no adapter is used in this class (and no gridView click listener)
 * Extras passed to target class (all in string resources):
 * extra_width, extra_height, extra_uri (if image is selected and isCorrectAfter with correct editTexts)
 * extra_id (if isCorrectAtFirst with correct editTexts, then call getImageIdExtra) and possibly extra_blank (getPosBlankExtra)
 * The number of abstract methods grow as I want to provide more functionality. If ever grown out of control shall be replaced with normal and util classes
 * Created by Neo on 7/23/2017.
 */
abstract class GenericGameParameter extends AppCompatActivity implements View.OnClickListener{
    private GridView mGridView;
    private Toast mToast; //to control toast for non-selected image
    private final int SELECT_IMG_REQUEST = 2;
    private Uri mUriImageSelected = null;
    private Button mOtherImageButton;
    private int mWidth, mHeight;
    private int [] width_Button_ids = new int[] {R.id.width_text, R.id.width_2, R.id.width_3, R.id.width_4, R.id.width_5};  //the first is always the text
    private int [] height_Button_ids = new int[] {R.id.height_text, R.id.height_2, R.id.height_3, R.id.height_4, R.id.height_5};
    private SparseArray<Button> widthMap, heightMap;
    private SparseBooleanArray globalMap;
    private Button mButtonWidthChosen = null, mButtonHeightChosen = null;
    private Button mButtonWidthText, mButtonHeightText;
    private int normalTextColor, normalBackgroundResource;
    private int ColorError1, ColorError2;

    /**
     * Method to be implemented: return which class to go next if all conditions are met
     * @return target class
     */
    abstract Class <?> classToJumpTo();

    /**
     * Method to be implemented: if true, we will check if width and height is set correctly before image selection and disable change after selection
     * @return if we should check if width and height is set correctly before image selection
     */
    abstract boolean widthHeightSetBeforeImageSelection();

    /**
     * Method to be implemented: Initializes the gridView
     * @param gridView: target gridView to be initialized (e.g. set up adapter)
     */
    abstract void initGridView(GridView gridView);

    /**
     * Method to be implemented: Check if gridView is valid at first (i.e. Before any selected image)
     * @param gridView: target gridView to be verified
     * @return if the gridView is valid
     */
    abstract boolean isCorrectAtFirst(GridView gridView); //e.g. position != -1 vs false

    /**
     * Method to be implemented: id extra passed to classToJumpTo() when isCorrectAtFirst and editTexts are correct
     * @param gridView: target gridView (which can encode imageId information)
     * @return the id of image, e.g. R.drawable.sample
     */
    abstract int getImageIdExtra(GridView gridView); // e.g. image[pos] vs do anything

    /**
     * Method to be implemented: blank extra passed to classToJumpTo()
     * @param gridView target gridView (which can encode posBlank information)
     * @return the position of blank symbol, -1 if don't want to pass anything
     */
    abstract int getPosBlankExtra(GridView gridView);

    /**
     * Method to be implemented: recreate code logic after image is selected
     * @param gridView: target gridView to be recreated (e.g. reset adapter)
     * @param bitmap: bitmap of the image selected
     * @param button: the button originally used to select a image
     * @param width : width of user input (ignore if you set editTextSetBeforeImageSelection to false
     * @param height: height of user input (ignore if you set editTextSetBeforeImageSelection to false
     */
    abstract void afterImageSelected(GridView gridView, Bitmap bitmap, Button button, int width, int height);

    /**
     * Method to be implemented:  Check if gridView is valid after the image is selected
     * @param gridView: target gridView to be verified
     * @return if the gridView is valid
     */
    abstract boolean isCorrectAfter(GridView gridView);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_parameter);

        ColorError1 = ContextCompat.getColor(this,R.color.colorAccent);
        ColorError2 = ContextCompat.getColor(this,R.color.error);

        //used the following 2 links for gallery help:
        //https://stackoverflow.com/questions/2169649/get-pick-an-image-from-androids-built-in-gallery-app-programmatically
        //https://stackoverflow.com/questions/3879992/how-to-get-bitmap-from-an-uri
        mOtherImageButton = (Button)findViewById(R.id.choose_other_image_button);
        mOtherImageButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                if (widthHeightSetBeforeImageSelection()) {
                    if (isCorrect(true)) {
                        mWidth = Integer.parseInt(mButtonWidthChosen.getText().toString());
                        mHeight = Integer.parseInt(mButtonHeightChosen.getText().toString());
                    } else {
                        if (mToast != null) {
                            mToast.cancel();
                        }
                        mToast = Toast.makeText(GenericGameParameter.this,
                                getResources().getString(R.string.text_before_image_error), Toast.LENGTH_SHORT);
                        mToast.show();
                        return;
                    }
                }


                // in onCreate or any event where your want the user to
                // select a file
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,
                        "Select Picture"), SELECT_IMG_REQUEST);
            }
        });

        mToast = null;

        widthMap = new SparseArray<>(width_Button_ids.length);
        heightMap = new SparseArray<>(height_Button_ids.length);
        globalMap = new SparseBooleanArray(width_Button_ids.length+height_Button_ids.length); // width: false, height: true
        for (int id : width_Button_ids) {
            Button button = (Button)findViewById(id);
            if (id == R.id.width_text) {
                mButtonWidthText = button;
            }
            button.setOnClickListener(this);
            widthMap.append(id, button);
            globalMap.append(id, false);
        }
        for (int id: height_Button_ids) {
            Button button = (Button)findViewById(id);
            if (id == R.id.height_text) {
                mButtonHeightText = button;
            }
            button.setOnClickListener(this);
            heightMap.append(id, button);
            globalMap.append(id, true);
        }
        normalTextColor = mButtonWidthText.getCurrentTextColor();
        normalBackgroundResource = android.R.drawable.btn_default;

        mGridView = (GridView) findViewById(R.id.image_selection_grids);
        initGridView(mGridView);
        mGridView.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return event.getAction() == MotionEvent.ACTION_MOVE;
            }
        });
    }

    public void onClick(View view) {
        int id = view.getId();
        if ((id == R.id.width_text) || (id == R.id.height_text)) {
            return;
        }
        if (!globalMap.get(id)) { //width
            Button newButtonChosen = widthMap.get(id);
            setFocus(newButtonChosen, mButtonWidthChosen, false);
            mButtonWidthChosen = newButtonChosen;
        } else {
            Button newButtonChosen = heightMap.get(id);
            setFocus(newButtonChosen, mButtonHeightChosen, true);
            mButtonHeightChosen = newButtonChosen;
        }
    }

    private void setFocus(Button new_btn, Button old_btn, boolean height){
        if (old_btn != null) {
            old_btn.setTextColor(normalTextColor);
            old_btn.setBackgroundResource(normalBackgroundResource);
        } else { //the first time, just reset text button
            Button button = (!height) ? mButtonWidthText : mButtonHeightText;
            button.setTextColor(normalTextColor);
            button.setBackgroundResource(normalBackgroundResource);
        }
        new_btn.setTextColor(ContextCompat.getColor(this, R.color.colorAccent));
        new_btn.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK)
        {
            Uri imageUri = data.getData();

            try {
                final Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                if (widthHeightSetBeforeImageSelection()) {
                    for(int i = 0; i < widthMap.size(); i++) {
                        Button button = widthMap.valueAt(i);
                        button.setOnClickListener(null);
                        button.setClickable(false);
                    }
                    for(int i = 0; i < heightMap.size(); i++) {
                        Button button = heightMap.valueAt(i);
                        button.setOnClickListener(null);
                        button.setClickable(false);
                    }
                }
                afterImageSelected(mGridView, bitmap, mOtherImageButton, mWidth, mHeight);
                mUriImageSelected = imageUri;
            }catch (Exception e) {
                Toast.makeText(this, getResources().getString(R.string.choose_other_image_error), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, getResources().getString(R.string.choose_other_image_error), Toast.LENGTH_LONG).show();
        }
    }
    
    private void setError(boolean widthCorrect, boolean heightCorrect) {
        if (!widthCorrect) {
            if ((mButtonWidthText.getCurrentTextColor() != ColorError1) && (mButtonWidthText.getCurrentTextColor() != ColorError2)) {
                mButtonWidthText.setTextColor(ColorError1);
                mButtonWidthText.setBackgroundColor(ColorError2);
            } else {
                int colorBackground = (mButtonWidthText.getCurrentTextColor() == ColorError1) ? ColorError2 : ColorError1;
                mButtonWidthText.setBackgroundColor(mButtonWidthText.getCurrentTextColor());
                mButtonWidthText.setTextColor(colorBackground);
            }
        }
        if (!heightCorrect) {
            if ((mButtonHeightText.getCurrentTextColor() != ColorError1) && (mButtonHeightText.getCurrentTextColor() != ColorError2)) {
                mButtonHeightText.setTextColor(ColorError1);
                mButtonHeightText.setBackgroundColor(ColorError2);
            } else {
                int colorBackground = (mButtonHeightText.getCurrentTextColor() == ColorError1) ? ColorError2 : ColorError1;
                mButtonHeightText.setBackgroundColor(mButtonHeightText.getCurrentTextColor());
                mButtonHeightText.setTextColor(colorBackground);
            }        
        }
    }

    static boolean ValidateInput(String str) {
        int property;
        try {
            property = Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return false;
        }
        return (property > 1 && property < 6);
    }



    /**
     * A function that checks each input and hint person if an input is invalid
     * @param textOnly if true, ignore image and only return if editTexts are set correctly
     * @return if all inputs are valid (thus can continue
     */
    private boolean isCorrect(boolean textOnly) {

        String widthStr = (mButtonWidthChosen==null) ? "-1" : mButtonWidthChosen.getText().toString();
        String heightStr = (mButtonHeightChosen==null) ? "-1" : mButtonHeightChosen.getText().toString();
        boolean widthCorrect = ValidateInput(widthStr);
        boolean heightCorrect = ValidateInput(heightStr);
        setError(widthCorrect, heightCorrect);
        if (textOnly) {
            return widthCorrect && heightCorrect;
        }
        boolean imageCorrect = ((mUriImageSelected!=null) && (isCorrectAfter(mGridView)))
                || ((mUriImageSelected==null) &&(isCorrectAtFirst(mGridView)));
        if ((mUriImageSelected==null) && !isCorrectAtFirst(mGridView)) {
            if (mToast != null) {
                mToast.cancel();
            }
            mToast = Toast.makeText(this, getResources().getString(R.string.image_error), Toast.LENGTH_SHORT);
            mToast.show();
        } else if ((mUriImageSelected!=null) && !isCorrectAfter(mGridView)) {
            if (mToast != null) {
                mToast.cancel();
            }
            mToast = Toast.makeText(this, getResources().getString(R.string.after_image_error), Toast.LENGTH_SHORT);
            mToast.show();
        }
        return widthCorrect && heightCorrect && imageCorrect;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.game_parameter_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int menuItemSelected = item.getItemId();
        if ((menuItemSelected == R.id.next_step_menu_item) && isCorrect(false)) {
            Intent gameIntent = new Intent(this, classToJumpTo());
            int width = Integer.parseInt(mButtonWidthChosen.getText().toString());
            int height = Integer.parseInt(mButtonHeightChosen.getText().toString());
            int posBlank = getPosBlankExtra(mGridView);
            gameIntent.putExtra(getResources().getString(R.string.extra_width), width);
            gameIntent.putExtra(getResources().getString(R.string.extra_height), height);
            if (mUriImageSelected != null) {
                gameIntent.putExtra(getResources().getString(R.string.extra_uri), mUriImageSelected.toString());
            } else {
                gameIntent.putExtra(getResources().getString(R.string.extra_id), getImageIdExtra(mGridView));
            }
            if (posBlank >= 0 && posBlank < width*height) {
                gameIntent.putExtra(getResources().getString(R.string.extra_blank), posBlank);
            }
            gameIntent.putExtra(getResources().getString(R.string.extra_start), (int [])null);
            gameIntent.putExtra(getResources().getString(R.string.extra_goal),  (int [])null);
            startActivity(gameIntent);
        } else if (menuItemSelected == R.id.prev_step_menu_item) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }


    class SingleImageAdapter extends BaseAdapter {
        Context mContext;
        Bitmap mBitmap;
        SingleImageAdapter(Context c, Bitmap b) {
            mContext = c;
            mBitmap = b;
        }

        @Override
        public int getCount() {
            return 1;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;

            // if it's not recycled, initialize some attributes
            if (convertView == null) {
                imageView = new ImageView(mContext);
                imageView.setLayoutParams(new GridView.LayoutParams(parent.getWidth(), parent.getHeight()));
                //imageView.setAdjustViewBounds(true);
                imageView.setScaleType(ImageView.ScaleType.CENTER);
            } else {
                imageView = (ImageView) convertView;
            }

            if ((mBitmap.getWidth() != parent.getWidth()) || (mBitmap.getHeight() != parent.getHeight())) {
                mBitmap =  Bitmap.createScaledBitmap(mBitmap, parent.getWidth(), parent.getHeight(), false);
            }
            imageView.setImageBitmap(mBitmap);
            return imageView;
        }
    }

}
