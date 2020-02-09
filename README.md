# Export Files with Clipnames from Davinci Resolve
This is a simple program, that renames and copys the clips in to a folder with the clipnames in Davinci Resolve.

# The Problem
It is a BAD! idea to rename your source files, because it can break other workflows in different programms. That is the reason why DaVinci doesn't allow it. But there are certain usecases, where it makes sense.

# My situation
In my case I already edited a whole shortmovie with seven shooting days and renamed all clips and audiofiles by hand, because we didn't have any edit/sound report and I needed these informations to cut according to the storyboard.
I didn't synced the sound in the beginning because I had problems with the DaVinci Resolves autosync via waveform. It looked like I sync by hand after editing the movie, because there is a feature where you directly connect the external audiofile with the videoclip. It turned out both of these functionalities are broken in the current version and shouldn' be used.  
Now our soundguy needs the information which takes we used and how the connected audio files are named. It was quite easy to put the clipname in the burnin, but DaVinci Resolve wouldn't let me display the actual filename of the associated audiofile, which meant that the easiest way to give all the informations to the soundguy would be to rename all the files to the clipnames and have a mapping file, where I could look for the original name in case I needed to. 

# The Solutions
You can export the metadata in DaVinci Resolve under File -> Export Metadata from -> Mediapool. This would work perfectly for normal clips. The column "EDL Clip Name" exports the displayed clipname. BUT! for audiofiles DaVinci Resolve wouldn't let me export via this feature. The only metadata column that gets exported is "Sound Roll #", which means I have to find the names from the postgres database.

# How to use it
- You need the metadata.csv
- You need a outputfolder

-> Done!

# Technology
- Java 11
- Maven
- OpenCSV
