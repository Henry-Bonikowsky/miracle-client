# 2026-01-04 | Miracle Client - Auto-Cinematic Clip System

DONE: Implemented complete auto-clip system for background videos (Phases 1-4, 6-7). Replay buffer captures 60s of gameplay, camera path generation creates orbit/chase cinematics, auto-clip system generates 30s clips every 10 minutes, clip manager handles 10-clip limit with favorites. All compiling and integrated.

FILES: ReplayFrame.java, EntitySnapshot.java, ReplayBufferManager.java, HighlightEvent.java, HighlightDetector.java, CameraStyle.java, CameraKeyframe.java, CameraPath.java, CameraPathGenerator.java, VirtualCamera.java, ReplayRenderer.java, ClipMetadata.java, ClipManager.java, AutoClipSystem.java, AutoClip.java, MiracleClient.java (integrated all systems), ClientPlayerEntityMixin.java, CameraMixin.java (virtual camera support), DirectoryManager.java (background-videos dir), ModuleManager.java (registered AutoClip)

NEXT: Phase 5 - Video encoding with FFmpeg. Need to integrate JavaCV/FFmpeg for actual MP4 rendering (currently just logs "Would render..."). ReplayRenderer has TODO markers for frame capture and encoding.

CONTEXT: 
- User wants Overwatch/Valorant-style background videos for launcher/menu
- Auto-generate every 10 minutes, 30 second clips, mix of orbit + chase camera
- Save to AppData/MiracleClient/background-videos/
- Keep last 10 clips, favorites don't count toward limit
- Highlight detection system built but not needed for this use case
- JavaCV dependency issue from earlier - need to resolve for video encoding
