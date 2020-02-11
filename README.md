# Export Files with Clipnames from Davinci Resolve
A simple program, that renames and copys the clips in to a folder with the clipnames in Davinci Resolve.

## The Problem
It is a **_BAD_** idea to rename your source files, because it can break other workflows in different programms. That is the reason why DaVinci doesn't allow it. But there are certain usecases, where it makes sense.

## My situation
In my case I had already edited a whole shortmovie with seven days of shooting and renamed all clips and audiofiles by hand. We didn't have any edit/sound reports and I needed to edit it according to the storyboard.
I didn't synced the sound in the beginning because I had problems with DaVinci Resolves "Sync via Waveform"-Feature. I didn't found it too disconcerning, because there is a feature where you directly connect the external audiofile with the videoclip. I thought I could easily do it after editing.
It turned out both of these features are broken in the current version and shouldn't be used. So there I was with broken sound and in need of a quick solution.
After I talked to our mixer, he said he only needs the information which takes we used and what the fitting audio files are and then could fix the sound. 
I already had all files renamed, which meant that the best way would be to rename all the files to the clipnames. It was quite easy to put the clipnames of the videofiles on the video with the "Data-Burnin" Feature.
But DaVinci Resolve just wouldn't let me export the display name of the files.

## The solutions
### metadata.csv:
You can export the metadata in DaVinci Resolve under File -> Export Metadata from -> Mediapool. This would work pretty well for normal clips. The column "EDL Clip Name" exports the displayed clipname. **BUT** for audiofiles Resolve doesn't export the "EDL Clip Name". The only metadata column for sound files that gets exported is "Sound Roll #".

### PostgreSQL Database:
DaVinci also has a feature where multiple people can edit on a database. Which obviously means that somewhere in it are the actual display names. After alot of trial and error I figured out where to find the fields I was looking for:
- The source name is in the table "Sm2MPMedia.Name" as text.
- The display names is in the table "Sm2MpMedia.FieldsBlob" in a base64 encoded blob. 
- The audio directory is in "BtAudioInfo.Clip" in a base64 encoded blob.
- The video directory is in "BtVideoInfo.Clip" in a base64 encoded blob.

## How to use it

For audio clips:
- You need a .pgpass file
- You need a outputfolder

For video clips:
- You need the metadata.csv
- You need a outputfolder

-> Done!

## Technology
- Java 11
- Maven
- PostgreSQL
- OpenCSV
