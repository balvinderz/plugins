package io.flutter.plugins.videoplayer;

import static com.google.android.exoplayer2.Player.REPEAT_MODE_ALL;
import static com.google.android.exoplayer2.Player.REPEAT_MODE_OFF;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.view.Surface;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Player.EventListener;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.RendererCapabilities;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.ui.TrackNameProvider;
import com.google.android.exoplayer2.ui.DefaultTrackNameProvider;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import io.flutter.plugin.common.EventChannel;
import io.flutter.view.TextureRegistry;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class VideoPlayer {
  private static final String FORMAT_SS = "ss";
  private static final String FORMAT_DASH = "dash";
  private static final String FORMAT_HLS = "hls";
  private static final String FORMAT_OTHER = "other";

  private SimpleExoPlayer exoPlayer;

  private Surface surface;

  private final TextureRegistry.SurfaceTextureEntry textureEntry;

  private QueuingEventSink eventSink = new QueuingEventSink();
  DefaultTrackSelector trackSelector ;

  private final EventChannel eventChannel;

  private boolean isInitialized = false;
  Context context;

  VideoPlayer(
      Context context,
      EventChannel eventChannel,
      TextureRegistry.SurfaceTextureEntry textureEntry,
      String dataSource,
      String formatHint) {
    this.eventChannel = eventChannel;
    this.context = context;

    this.textureEntry = textureEntry;

    trackSelector = new DefaultTrackSelector();
    exoPlayer = ExoPlayerFactory.newSimpleInstance(context, trackSelector);

    Uri uri = Uri.parse(dataSource);

    DataSource.Factory dataSourceFactory;
    if (isHTTP(uri)) {
      dataSourceFactory =
          new DefaultHttpDataSourceFactory(
              "ExoPlayer",
              null,
              DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
              DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS,
              true);
    } else {
      dataSourceFactory = new DefaultDataSourceFactory(context, "ExoPlayer");
    }

    MediaSource mediaSource = buildMediaSource(uri, dataSourceFactory, formatHint, context);
    exoPlayer.prepare(mediaSource);

    setupVideoPlayer(eventChannel, textureEntry);
  }

  private static boolean isHTTP(Uri uri) {
    if (uri == null || uri.getScheme() == null) {
      return false;
    }
    String scheme = uri.getScheme();
    return scheme.equals("http") || scheme.equals("https");
  }

  private MediaSource buildMediaSource(
      Uri uri, DataSource.Factory mediaDataSourceFactory, String formatHint, Context context) {
    int type;
    if (formatHint == null) {
      type = Util.inferContentType(uri.getLastPathSegment());
    } else {
      switch (formatHint) {
        case FORMAT_SS:
          type = C.TYPE_SS;
          break;
        case FORMAT_DASH:
          type = C.TYPE_DASH;
          break;
        case FORMAT_HLS:
          type = C.TYPE_HLS;
          break;
        case FORMAT_OTHER:
          type = C.TYPE_OTHER;
          break;
        default:
          type = -1;
          break;
      }
    }
    switch (type) {
      case C.TYPE_SS:
        return new SsMediaSource.Factory(
                new DefaultSsChunkSource.Factory(mediaDataSourceFactory),
                new DefaultDataSourceFactory(context, null, mediaDataSourceFactory))
            .createMediaSource(uri);
      case C.TYPE_DASH:
        return new DashMediaSource.Factory(
                new DefaultDashChunkSource.Factory(mediaDataSourceFactory),
                new DefaultDataSourceFactory(context, null, mediaDataSourceFactory))
            .createMediaSource(uri);
      case C.TYPE_HLS:
        return new HlsMediaSource.Factory(mediaDataSourceFactory).createMediaSource(uri);
      case C.TYPE_OTHER:
        return new ExtractorMediaSource.Factory(mediaDataSourceFactory)
            .setExtractorsFactory(new DefaultExtractorsFactory())
            .createMediaSource(uri);
      default:
        {
          throw new IllegalStateException("Unsupported type: " + type);
        }
    }
  }

  private void setupVideoPlayer(
      EventChannel eventChannel, TextureRegistry.SurfaceTextureEntry textureEntry) {

    eventChannel.setStreamHandler(
        new EventChannel.StreamHandler() {
          @Override
          public void onListen(Object o, EventChannel.EventSink sink) {
            eventSink.setDelegate(sink);
          }

          @Override
          public void onCancel(Object o) {
            eventSink.setDelegate(null);
          }
        });

    surface = new Surface(textureEntry.surfaceTexture());
    exoPlayer.setVideoSurface(surface);
    setAudioAttributes(exoPlayer);

    exoPlayer.addListener(
        new EventListener() {

          @Override
          public void onPlayerStateChanged(final boolean playWhenReady, final int playbackState) {
            if (playbackState == Player.STATE_BUFFERING) {
              sendBufferingUpdate();
            } else if (playbackState == Player.STATE_READY) {
              if (!isInitialized) {
                isInitialized = true;
                sendInitialized();
              }
            } else if (playbackState == Player.STATE_ENDED) {
              Map<String, Object> event = new HashMap<>();
              event.put("event", "completed");
              eventSink.success(event);
            }
          }

          @Override
          public void onPlayerError(final ExoPlaybackException error) {
            if (eventSink != null) {
              eventSink.error("VideoError", "Video player had error " + error, null);
            }
          }
        });
  }

  void sendBufferingUpdate() {
    Map<String, Object> event = new HashMap<>();
    event.put("event", "bufferingUpdate");
    List<? extends Number> range = Arrays.asList(0, exoPlayer.getBufferedPosition());
    // iOS supports a list of buffered ranges, so here is a list with a single range.
    event.put("values", Collections.singletonList(range));
    eventSink.success(event);
  }

  @SuppressWarnings("deprecation")
  private static void setAudioAttributes(SimpleExoPlayer exoPlayer) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      exoPlayer.setAudioAttributes(
          new AudioAttributes.Builder().setContentType(C.CONTENT_TYPE_MOVIE).build());
    } else {
      exoPlayer.setAudioStreamType(C.STREAM_TYPE_MUSIC);
    }
  }

  void play() {
    exoPlayer.setPlayWhenReady(true);
  }

  void pause() {
    exoPlayer.setPlayWhenReady(false);
  }

  void setLooping(boolean value) {
    exoPlayer.setRepeatMode(value ? REPEAT_MODE_ALL : REPEAT_MODE_OFF);
  }

  void setVolume(double value) {
    float bracketedValue = (float) Math.max(0.0, Math.min(1.0, value));
    exoPlayer.setVolume(bracketedValue);
  }
  ArrayList getAudios() {
    MappingTrackSelector.MappedTrackInfo mappedTrackInfo =
            trackSelector.getCurrentMappedTrackInfo();

    ArrayList audios = new ArrayList();

    for(int i =0;i<mappedTrackInfo.getRendererCount();i++)
    {
      if(mappedTrackInfo.getRendererType(i)!= C.TRACK_TYPE_AUDIO)
        continue;

      TrackGroupArray trackGroupArray = mappedTrackInfo.getTrackGroups(i);
      for(int j =0;j<trackGroupArray.length;j++) {

        TrackGroup group = trackGroupArray.get(j);
        TrackNameProvider provider = new DefaultTrackNameProvider(context.getResources());
        for (int k = 0; k < group.length; k++) {
          if ((mappedTrackInfo.getTrackSupport(i, j, k) &0b111)
                  == RendererCapabilities.FORMAT_HANDLED) {
            //trackSelector.setParameters(builder);
            audios.add(provider.getTrackName(group.getFormat(k)));


          }

        }
      }

    }
    return audios;


  }
  void setAudio( String audioName) {
    MappingTrackSelector.MappedTrackInfo mappedTrackInfo =
            trackSelector.getCurrentMappedTrackInfo();

    StringBuilder str = new StringBuilder();

    for (int i = 0; i < mappedTrackInfo.getRendererCount(); i++) {
      if (mappedTrackInfo.getRendererType(i) != C.TRACK_TYPE_AUDIO)
        continue;

      TrackGroupArray trackGroupArray = mappedTrackInfo.getTrackGroups(i);
      for (int j = 0; j < trackGroupArray.length; j++) {

        TrackGroup group = trackGroupArray.get(j);
        TrackNameProvider provider = new DefaultTrackNameProvider(context.getResources());
        for (int k = 0; k < group.length; k++) {

          if (provider.getTrackName(group.getFormat(k)).equals(audioName)) {

            DefaultTrackSelector.ParametersBuilder builder = trackSelector.getParameters().buildUpon();
            builder.clearSelectionOverrides(i).setRendererDisabled(i, false);
            int[] tracks = {k};
            DefaultTrackSelector.SelectionOverride override = new DefaultTrackSelector.SelectionOverride(j, tracks);
            builder.setSelectionOverride(i, mappedTrackInfo.getTrackGroups(i), override);
            trackSelector.setParameters(builder);
            return ;



          }

        }
      }

    }
  }

  void seekTo(int location) {
    exoPlayer.seekTo(location);
  }

  long getPosition() {
    return exoPlayer.getCurrentPosition();
  }

  @SuppressWarnings("SuspiciousNameCombination")
  private void sendInitialized() {
    if (isInitialized) {
      Map<String, Object> event = new HashMap<>();
      event.put("event", "initialized");
      event.put("duration", exoPlayer.getDuration());

      if (exoPlayer.getVideoFormat() != null) {
        Format videoFormat = exoPlayer.getVideoFormat();
        int width = videoFormat.width;
        int height = videoFormat.height;
        int rotationDegrees = videoFormat.rotationDegrees;
        // Switch the width/height if video was taken in portrait mode
        if (rotationDegrees == 90 || rotationDegrees == 270) {
          width = exoPlayer.getVideoFormat().height;
          height = exoPlayer.getVideoFormat().width;
        }
        event.put("width", width);
        event.put("height", height);
      }
      eventSink.success(event);
    }
  }

  void dispose() {
    if (isInitialized) {
      exoPlayer.stop();
    }
    textureEntry.release();
    eventChannel.setStreamHandler(null);
    if (surface != null) {
      surface.release();
    }
    if (exoPlayer != null) {
      exoPlayer.release();
    }
  }
}
