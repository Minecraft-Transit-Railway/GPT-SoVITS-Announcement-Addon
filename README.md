# GPT-SoVITS Announcement Addon

This addon uses the [GPT-SoVITS](https://github.com/RVC-Boss/GPT-SoVITS) project ([MIT License](https://github.com/RVC-Boss/GPT-SoVITS/blob/main/LICENSE)) to synthesize and play multilingual next station announcements.
Players riding vehicles on any public Minecraft Transit Railway server can be tracked, and the next station announcements can be played in real time.

## System Requirements and Limitations

- Windows (other operating systems are not supported yet)
- At least 20–25 GB of free space
- Java 17
- A good CPU and GPU for near-realtime TTS synthesis

## Installation

1. Download the artifact from the latest [GitHub Actions](https://github.com/Minecraft-Transit-Railway/GPT-SoVITS-Announcement-Addon/actions) run.
2. Unzip the artifact into a new folder.
3. Open a terminal in the new folder and run the command
   ```
   java -jar GPT-SoVITS-Announcement-Addon.jar
   ```
4. The last line in the terminal should now contain `Angular application ready [http://localhost:8080]`. Open this link in your browser.
5. The web UI should open. Click on the "Install" button on the top left; this might take a few minutes.

## Usage

### Adding Voices

1. The "Add Voices" section is for registering pre-trained models. Prepare or obtain the following files:
    - The GPT weights file (`.ckpt`)
    - The SoVITS weights file (`.pth`)
    - A sound sample (`.mp3`, `.wav`, etc.). The sound sample is a short 3–10-second clip of the same person talking. It is used to shape the tone, emotion, and inflections.
2. Now, create a JSON representation of voice templates using the above files and additional metadata. An example is shown below:
   ```json
   [
       {
           "id": "mtr-canto",
           "runtimeCode": "yue",
           "ckptPath": "C:\\models\\mtr-canto-e10.ckpt",
           "pthPath": "C:\\models\\mtr-canto_e4_s100.pth",
           "voiceSamplePath": "C:\\samples\\sih-canto.mp3",
           "voiceSampleText": "下一站：兆康。乘客可以轉乘輕鐵。"
       },
       {
           "id": "mtr-mandarin",
           "runtimeCode": "zh",
           "ckptPath": "C:\\models\\mtr-mandarin-e10.ckpt",
           "pthPath": "C:\\models\\mtr-mandarin_e4_s100.pth",
           "voiceSamplePath": "C:\\samples\\nop-mandarin.mp3",
           "voiceSampleText": "下一站：北角。乘客可以換乘將軍澳綫。"
       },
       {
           "id": "mtr-english",
           "runtimeCode": "en",
           "ckptPath": "C:\\models\\mtr-english-e10.ckpt",
           "pthPath": "C:\\models\\mtr-english_e4_s92.pth",
           "voiceSamplePath": "C:\\samples\\sih-english.mp3",
           "voiceSampleText": "Next station, Siu Hong. Interchange station for the light rail."
       }
   ]
   ```
    - `id`: A unique ID of the model.
    - `runtimeCode`: The code representing the language of the model. The following languages are supported:
        - `yue`: Cantonese
        - `zh`: Mandarin
        - `en`: English
        - `ja`: Japanese
        - `ko`: Korean
    - `ckptPath`: The path to the GPT weights file (`.ckpt`). Note that Windows file paths use the backslash which is represented by `\\` in JSON.
    - `pthPath`: The path to the SoVITS weights file (`.pth`). Note that Windows file paths use the backslash which is represented by `\\` in JSON.
    - `voiceSamplePath`: The path to the sound sample. Note that Windows file paths use the backslash which is represented by `\\` in JSON.
    - `voiceSampleText`: What is being said in the voice sample.
3. Paste this JSON into the "Voice Templates" box and click on "Add".

### Testing Voices

1. After adding voices in the previous section, use the "Text To Speech Synthesis Testing" section to test the voices. Create a JSON representation of a synthesis request. An example is shown below:
   ```json
   [
       {
           "voiceId": "mtr-canto",
           "text": "下一站,岩漿礦坑。乘客可以轉乘一號美岩綫，麗灣特快，或礦鐵岩羽綫。"
       },
       {
           "voiceId": "mtr-mandarin",
           "text": "下一站,岩漿礦坑。乘客可以換乘一號美岩綫，麗灣特快，或礦鐵岩羽綫。"
       },
       {
           "voiceId": "mtr-english",
           "text": "Next station, Yanjiang Mineshaft. Interchange station for the Line 1 Meyan Line, Kallos Express, and Wilds Railway Yanyu Line."
       }
   ]
   ```
    - `voiceId`: The unique ID of the model (defined previously).
    - `text`: The text to synthesize.
2. Paste this JSON into the "Synthesis Request" box and click on "Submit".
3. After text synthesis is done, click on "Play".

### Minecraft Transit Railway Player Tracking

1. To track players in real time on a Minecraft Transit Railway server, create a JSON representation of a player tracking request. Note that the Transport System Map of the server must be publicly accessible from the current computer. An example is shown below:
   ```json
   {
       "serverUrl": "https://letsplay.minecrafttransitrailway.com/system-map",
       "dimension": 0,
       "player": "Jon_Ho",
       "announcements": [
           {
               "voiceId": "mtr-canto",
               "nextStationNoInterchange": "下一站,%s。",
               "nextStationInterchange": "下一站,%s。乘客可以轉乘%s。",
               "joinLast": "或"
           },
           {
               "voiceId": "mtr-mandarin",
               "nextStationNoInterchange": "下一站,%s。",
               "nextStationInterchange": "下一站,%s。乘客可以換乘%s。",
               "joinLast": "或"
           },
           {
               "voiceId": "mtr-english",
               "nextStationNoInterchange": "Next station, %s.",
               "nextStationInterchange": "Next station, %s. Interchange station for the %s.",
               "joinLast": "and"
           }
       ]
   }
   ```
    - `serverUrl`: The Transport System Map URL.
    - `dimension`: The index of the dimension. Usually the Overworld is `0`, The Nether is `1`, and The End is `2`.
    - `player`: The player to track. This can be a UUID or player name.
    - `announcements`: The voice and format settings of the next station announcement that should be played.
        - `voiceId`: The unique ID of the model (defined previously).
        - `nextStationNoInterchange`: The format of the next station announcement that will be played when there are no interchanges. There must be one `%s`, which will be replaced by the station name.
        - `nextStationInterchange`: The format of the next station announcement that will be played when there are interchanges. There must be two `%s`; the first one will be replaced by the station name and the second one will be replaced by the list of interchange routes.
        - `joinLast`: The conjunction to join the last two interchange routes. For example, if the interchange routes are "Line 1", "Line 2", and "Line 3" and `joinLast` is "and", the resulting phrase will be "Line 1, Line 2, and Line 3".
2. Paste this JSON into the "Player Tracking Request" box and click on "Submit".

It is recommended to run this addon on a separate computer as Minecraft itself, as the text-to-speech synthesis might fight for CPU and GPU resources with the Minecraft process.

## Additional Resources

To create your own GPT-SoVITS models, check out [this tutorial](https://www.yuque.com/baicaigongchang1145haoyuangong/ib3g1e/xyyqrfwiu3e2bgyk).
