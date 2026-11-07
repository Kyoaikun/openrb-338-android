use std::sync::{Arc, Mutex, OnceLock};
use jni::{JNIEnv, objects::{JClass, JByteArray, JShortArray}, sys::{jboolean, jint}};
use orb_engine::{tables::Tables, song::Device};
use orb_player::{Audio, Player, Command};
struct State { player: Player, audio: Audio }
static STATE: OnceLock<Mutex<State>> = OnceLock::new();
fn state() -> &'static Mutex<State> { STATE.get_or_init(|| {let (player,audio)=Player::new(Arc::new(Tables::new()),Some(rbm::standard::builtin_samples()));Mutex::new(State{player,audio})})}
fn with_state<F:FnOnce(&mut State)>(f:F){if let Ok(mut s)=state().lock(){f(&mut s);}}
#[no_mangle] pub extern "system" fn Java_org_openrb_mobile_MainActivity_nativeInit(_:JNIEnv,_:JClass){let _=state();}
#[no_mangle] pub extern "system" fn Java_org_openrb_mobile_MainActivity_nativeCommand(_:JNIEnv,_:JClass,kind:jint,arg:jint){with_state(|s|{let c=match kind{0=>Command::Play,1=>Command::Stop,2=>Command::Tempo((arg.clamp(40,300) as u32)*1000),3=>Command::Shuffle(arg.clamp(0,127) as u8),4=>Command::SongMode(arg!=0),5=>Command::LoopOn(arg!=0),_=>return};let _=s.player.send(c);s.player.collect_garbage();});}
#[no_mangle] pub extern "system" fn Java_org_openrb_mobile_MainActivity_nativeParam(_:JNIEnv,_:JClass,device:jint,ctrl:jint,value:jint){if let Some(device)=Device::from_id(device as u8){with_state(|s|{let _=s.player.send(Command::DeviceParam{device,ctrl:ctrl as u8,value:value.clamp(0,127) as u8});s.player.collect_garbage();});}}
#[no_mangle] pub extern "system" fn Java_org_openrb_mobile_MainActivity_nativeLoad(mut env:JNIEnv,_:JClass,bytes:JByteArray)->jboolean{let Ok(bytes)=env.convert_byte_array(bytes)else{return 0;};let Ok(song)=rbs::Song::parse(&bytes)else{return 0;};let Ok(song)=orb_engine::load::convert(&song)else{return 0;};let mut ok=false;with_state(|s|{ok=s.player.load(&song,Some(rbm::standard::builtin_samples())).is_ok();s.player.collect_garbage();});ok as jboolean}
#[no_mangle] pub extern "system" fn Java_org_openrb_mobile_MainActivity_nativeRender(mut env:JNIEnv,_:JClass,buffer:JShortArray)->jint{let Ok(len)=env.get_array_length(&buffer)else{return 0;};let mut pcm=vec![0i16;len as usize];with_state(|s|s.audio.render_i16(&mut pcm));if env.set_short_array_region(&buffer,0,&pcm).is_err(){return 0;}len}
