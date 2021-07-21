package com.zero.print;

public class Printer {
    //HW
    public static final byte[] BEEPER               = {0x1b, 0x42, 0x05, 0x05};
    public static final byte[] INIT                 = {0x1b, 0x40};
    //TEXT
    public static final byte[] ALIGN_LEFT           = {0x1b, 0x61, 0x00};
    public static final byte[] ALIGN_CENTER         = {0x1b, 0x61, 0x01};
    public static final byte[] ALIGN_RIGHT          = {0x1b, 0x61, 0x02};
    public static final byte[] SIZE_NORMAL          = {0x1b, 0x21, 0x03};
    public static final byte[] SIZE_MEDIUM          = {0x1b, 0x21, 0x08};
    public static final byte[] SIZE_LARGE           = {0x1b, 0x21, 0x30};
    public static final byte[] BOLD_OFF             = {0x1b, 0x45, 0x00};
    public static final byte[] BOLD_ON              = {0x1b, 0x45, 0x01};
    //PAPER
    public static final byte[] PAPER_FULL_CUT       = {0x1d, 0x56, 0x00};
    public static final byte[] PAPER_PART_CUT       = {0x1d, 0x56, 0x01};
    public static final byte[] PAPER_FEED_AND_CUT   = {0x1D, 0x56, 66, 0x00};
    public static final byte[] PAPER_FEED           = {0x0a};
}