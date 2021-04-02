package ar.com.craving.ffmpeg;

public class EncodingStructure {
    String definedName;
    String videoResolution;
    String videoCodec;
    String videoBitrate;
    String videoProfile;
    String videoLevel;
    String videoFps;
    String videoPreset;
    String audioCodec;
    String audioBitrate;
    String audioSample;
    String audioChannel;
    

    public EncodingStructure() {
    }

    public EncodingStructure(String definedName, String videoResolution, String videoCodec, String videoBitrate, String videoProfile, String videoLevel, String videoFps, String videoPreset, String audioCodec, String audioBitrate, String audioSample, String audioChannel) {
        this.definedName = definedName;
        this.videoResolution = videoResolution;
        this.videoCodec = videoCodec;
        this.videoBitrate = videoBitrate;
        this.videoProfile = videoProfile;
        this.videoLevel = videoLevel;
        this.videoFps = videoFps;
        this.videoPreset = videoPreset;
        this.audioCodec = audioCodec;
        this.audioBitrate = audioBitrate;
        this.audioSample = audioSample;
        this.audioChannel = audioChannel;
    }

    public String getDefinedName() {
        return this.definedName;
    }

    public void setDefinedName(String definedName) {
        this.definedName = definedName;
    }

    public String getVideoResolution() {
        return this.videoResolution;
    }

    public void setVideoResolution(String videoResolution) {
        this.videoResolution = videoResolution;
    }

    public String getVideoCodec() {
        return this.videoCodec;
    }

    public void setVideoCodec(String videoCodec) {
        this.videoCodec = videoCodec;
    }

    public String getVideoBitrate() {
        return this.videoBitrate;
    }

    public void setVideoBitrate(String videoBitrate) {
        this.videoBitrate = videoBitrate;
    }

    public String getVideoProfile() {
        return this.videoProfile;
    }

    public void setVideoProfile(String videoProfile) {
        this.videoProfile = videoProfile;
    }

    public String getVideoLevel() {
        return this.videoLevel;
    }

    public void setVideoLevel(String videoLevel) {
        this.videoLevel = videoLevel;
    }

    public String getVideoFps() {
        return this.videoFps;
    }

    public void setVideoFps(String videoFps) {
        this.videoFps = videoFps;
    }

    public String getVideoPreset() {
        return this.videoPreset;
    }

    public void setVideoPreset(String videoPreset) {
        this.videoPreset = videoPreset;
    }

    public String getAudioCodec() {
        return this.audioCodec;
    }

    public void setAudioCodec(String audioCodec) {
        this.audioCodec = audioCodec;
    }

    public String getAudioBitrate() {
        return this.audioBitrate;
    }

    public void setAudioBitrate(String audioBitrate) {
        this.audioBitrate = audioBitrate;
    }

    public String getAudioSample() {
        return this.audioSample;
    }

    public void setAudioSample(String audioSample) {
        this.audioSample = audioSample;
    }

    public String getAudioChannel() {
        return this.audioChannel;
    }

    public void setAudioChannel(String audioChannel) {
        this.audioChannel = audioChannel;
    }

    public EncodingStructure definedName(String definedName) {
        this.definedName = definedName;
        return this;
    }

    public EncodingStructure videoResolution(String videoResolution) {
        this.videoResolution = videoResolution;
        return this;
    }

    public EncodingStructure videoCodec(String videoCodec) {
        this.videoCodec = videoCodec;
        return this;
    }

    public EncodingStructure videoBitrate(String videoBitrate) {
        this.videoBitrate = videoBitrate;
        return this;
    }

    public EncodingStructure videoProfile(String videoProfile) {
        this.videoProfile = videoProfile;
        return this;
    }

    public EncodingStructure videoLevel(String videoLevel) {
        this.videoLevel = videoLevel;
        return this;
    }

    public EncodingStructure videoFps(String videoFps) {
        this.videoFps = videoFps;
        return this;
    }

    public EncodingStructure videoPreset(String videoPreset) {
        this.videoPreset = videoPreset;
        return this;
    }

    public EncodingStructure audioCodec(String audioCodec) {
        this.audioCodec = audioCodec;
        return this;
    }

    public EncodingStructure audioBitrate(String audioBitrate) {
        this.audioBitrate = audioBitrate;
        return this;
    }

    public EncodingStructure audioSample(String audioSample) {
        this.audioSample = audioSample;
        return this;
    }

    public EncodingStructure audioChannel(String audioChannel) {
        this.audioChannel = audioChannel;
        return this;
    }

   

    @Override
    public String toString() {
        return "{" +
            " definedName='" + getDefinedName() + "'" +
            ", videoResolution='" + getVideoResolution() + "'" +
            ", videoCodec='" + getVideoCodec() + "'" +
            ", videoBitrate='" + getVideoBitrate() + "'" +
            ", videoProfile='" + getVideoProfile() + "'" +
            ", videoLevel='" + getVideoLevel() + "'" +
            ", videoFps='" + getVideoFps() + "'" +
            ", videoPreset='" + getVideoPreset() + "'" +
            ", audioCodec='" + getAudioCodec() + "'" +
            ", audioBitrate='" + getAudioBitrate() + "'" +
            ", audioSample='" + getAudioSample() + "'" +
            ", audioChannel='" + getAudioChannel() + "'" +
            "}";
    }
    

}
