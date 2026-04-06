package fr.segame.armesiaLevel.config;

/**
 * Représente une action exécutée lors de l'atteinte d'un palier de niveau.
 */
public class MilestoneAction {

    public enum Type {
        MESSAGE, BROADCAST, TITLE, SOUND, COMMAND
    }

    private final Type   type;
    private final String value;
    private final String subtitle;
    private final String soundId;
    private final float  volume;
    private final float  pitch;
    private final int    fadeIn;
    private final int    stay;
    private final int    fadeOut;

    public MilestoneAction(Type type, String value, String subtitle,
                           String soundId, float volume, float pitch,
                           int fadeIn, int stay, int fadeOut) {
        this.type     = type;
        this.value    = value;
        this.subtitle = subtitle;
        this.soundId  = soundId;
        this.volume   = volume;
        this.pitch    = pitch;
        this.fadeIn   = fadeIn;
        this.stay     = stay;
        this.fadeOut  = fadeOut;
    }

    public Type   getType()     { return type; }
    public String getValue()    { return value; }
    public String getSubtitle() { return subtitle; }
    public String getSoundId()  { return soundId; }
    public float  getVolume()   { return volume; }
    public float  getPitch()    { return pitch; }
    public int    getFadeIn()   { return fadeIn; }
    public int    getStay()     { return stay; }
    public int    getFadeOut()  { return fadeOut; }
}

