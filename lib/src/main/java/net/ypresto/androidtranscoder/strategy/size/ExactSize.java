package net.ypresto.androidtranscoder.strategy.size;

/**
 * A special {@link Size} that knows about which dimension is width
 * and which is height.
 */
public class ExactSize extends Size {

    private int mWidth;
    private int mHeight;

    public ExactSize(int width, int height) {
        super(width, height);
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }
}
