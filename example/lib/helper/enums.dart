final int instant = 0;
final int threeSec = 3;
final int tenSec = 10;

enum Flash { FLASH_ON, FLASH_OFF }
enum Camera { NO_CAMERA, FRONT_CAMERA, BACK_CAMERA }
enum Audio { SOUND_ON, SOUND_OFF, CUSTOM }
enum Delay { INSTANT, THREE_SEC, TEN_SEC }

/// Get delay seconds...
int getDelay(Delay _delay) {
  return _delay == Delay.INSTANT ? instant
      : _delay == Delay.THREE_SEC ? threeSec
      : tenSec;
}

/// To change the delay...
Delay setDelay(Delay _delay) {
  return _delay == Delay.INSTANT ? Delay.THREE_SEC : _delay == Delay.THREE_SEC
      ? Delay.TEN_SEC
      : Delay.INSTANT;
}
