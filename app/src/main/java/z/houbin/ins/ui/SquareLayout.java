package z.houbin.ins.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.squareup.picasso.Picasso;

import java.io.File;

import z.houbin.ins.R;
import z.houbin.ins.util.VideoUtil;

public class SquareLayout extends RelativeLayout {
    private boolean init;

    public SquareLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public SquareLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareLayout(Context context) {
        super(context);
    }


    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        setMeasuredDimension(getDefaultSize(0, widthMeasureSpec), getDefaultSize(0, heightMeasureSpec));

        int childWidthSize = getMeasuredWidth();
        int childHeightSize = getMeasuredHeight();

        heightMeasureSpec = widthMeasureSpec = MeasureSpec.makeMeasureSpec(childWidthSize, MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility == VISIBLE) {
            ImageView childImage = findViewById(R.id.image);
            File file = (File) getTag();
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                if (files == null || files.length == 0) {
                    return;
                } else {
                    file = files[0];
                }
            }
            if (file.getName().endsWith(".mp4")) {
                Bitmap bitmap = VideoUtil.getVideoThumbnail(file.getPath(), 512, 384, MediaStore.Images.Thumbnails.FULL_SCREEN_KIND);
                childImage.setImageBitmap(bitmap);
            } else {
                Picasso.get().load(file).into(childImage);
            }
            setInit(true);
        }
    }

    public boolean isInit() {
        return init;
    }

    public void setInit(boolean init) {
        this.init = init;
    }
}