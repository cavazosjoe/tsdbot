#!/bin/bash

# example shell script that loops over video files and does stuff
# might involve renaming them, pre-processing, filtering streams, etc
i=1;
for f in *.mkv;
do

        # start at a particular file maybe?
        if [ "$i" -gt 0 ] ; then

            # simple rename to standard format
            mv "$f" "$i--$f"

            # burn subtitles
            mkvextract tracks "$f" 4:subtitles.srt;
            VF="yadif,subtitles=subtitles.srt";
            ffmpeg -y -i "$f" -c:v libx264 -b:v 1500k -vf "$VF" -c:a aac -strict experimental -b:a 128k "/media/TSDHQ-HDD/tsdtv/The_Show/$i--$f";
            rm subtitles.srt;

            # filter tracks
            ffmpeg -y -i "$f" -map 0:0 -map 0:2 -c:v copy -c:a copy "/media/TSDHQ-HDD/tsdtv/The_Show/$i--$f";

        fi

        i=$((i+1));

done