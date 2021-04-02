package webVimeoDownloader;

public class Video {
    String link;
    int duration;
    int width;
    int height;
    String name;

    public Video(String link, int duration, int width, int height, String name) {
        this.link = link;
        this.duration = duration;
        this.width = width;
        this.height = height;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

}
