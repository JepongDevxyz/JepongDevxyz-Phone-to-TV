package com.jepongdevxyz.phonetotv;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.net.*;
import android.net.nsd.*;
import android.os.*;
import android.provider.OpenableColumns;
import android.view.*;
import android.widget.*;
import androidx.activity.result.*;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {
  static final String TYPE="_jepongfiles._tcp.";
  final ExecutorService io=Executors.newCachedThreadPool();
  final ArrayList<Uri> files=new ArrayList<>();
  final ArrayList<Peer> peers=new ArrayList<>();
  LinearLayout root,peerBox,fileBox; TextView status,codeText; ProgressBar progress;
  Button send,cancel,retry,pick; ImageView qr;
  NsdManager nsd; NsdManager.DiscoveryListener discovery; NsdManager.RegistrationListener registration;
  TransferServer server; Peer selected; volatile TransferJob current; String code;
  final ActivityResultLauncher<ScanOptions> scanner=registerForActivityResult(new ScanContract(),r->{if(r.getContents()!=null)pairQr(r.getContents());});
  final ActivityResultLauncher<Intent> picker=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),r->{
    if(r.getResultCode()!=RESULT_OK||r.getData()==null)return;
    Intent d=r.getData(); files.clear();
    if(d.getClipData()!=null) for(int i=0;i<d.getClipData().getItemCount();i++) files.add(d.getClipData().getItemAt(i).getUri());
    else if(d.getData()!=null) files.add(d.getData());
    renderFiles();
  });

  @Override public void onCreate(Bundle b){super.onCreate(b); getWindow().setStatusBarColor(Color.rgb(7,17,31)); buildUi(); startReceiver();}
  TextView text(String s,int sp){TextView v=new TextView(this);v.setText(s);v.setTextColor(Color.rgb(235,245,255));v.setTextSize(sp);v.setLineSpacing(0,1.08f);v.setPadding(0,dp(5),0,dp(5));return v;}
  TextView muted(String s,int sp){TextView v=text(s,sp);v.setTextColor(Color.rgb(145,169,193));return v;}
  Button button(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setTextSize(14);b.setAllCaps(false);b.setGravity(Gravity.CENTER);b.setFocusable(true);b.setMinHeight(dp(54));b.setBackgroundResource(com.jepongdevxyz.phonetotv.R.drawable.bg_secondary);b.setPadding(dp(14),0,dp(14),0);return b;}
  Button iconButton(String s,int icon,boolean primary){Button b=button(s);b.setCompoundDrawablesWithIntrinsicBounds(icon,0,0,0);b.setCompoundDrawablePadding(dp(10));if(primary)b.setBackgroundResource(com.jepongdevxyz.phonetotv.R.drawable.bg_primary);return b;}
  LinearLayout card(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);v.setBackgroundResource(com.jepongdevxyz.phonetotv.R.drawable.bg_card);v.setPadding(dp(18),dp(16),dp(18),dp(16));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(8),0,dp(10));v.setLayoutParams(lp);return v;}
  int dp(int x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
  void buildUi(){
    ScrollView sc=new ScrollView(this);sc.setFillViewport(true);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(16),dp(20),dp(36));root.setBackgroundColor(Color.rgb(7,17,31));sc.addView(root);setContentView(sc);
    LinearLayout brand=new LinearLayout(this);brand.setGravity(Gravity.CENTER_VERTICAL);ImageView logo=new ImageView(this);logo.setImageResource(com.jepongdevxyz.phonetotv.R.drawable.brand_logo);brand.addView(logo,new LinearLayout.LayoutParams(dp(52),dp(52)));LinearLayout brandText=new LinearLayout(this);brandText.setOrientation(LinearLayout.VERTICAL);brandText.setPadding(dp(12),0,0,0);TextView title=text("Send Files to TV",27);title.setTypeface(null,Typeface.BOLD);brandText.addView(title);brandText.addView(muted("Fast local transfer • No cloud required",13));brand.addView(brandText,new LinearLayout.LayoutParams(0,-2,1));root.addView(brand);
    LinearLayout receiveCard=card();LinearLayout receiveTop=new LinearLayout(this);receiveTop.setGravity(Gravity.CENTER_VERTICAL);LinearLayout labels=new LinearLayout(this);labels.setOrientation(LinearLayout.VERTICAL);labels.addView(muted("RECEIVE MODE",12));TextView ready=text("Ready to receive",20);ready.setTypeface(null,Typeface.BOLD);labels.addView(ready);receiveTop.addView(labels,new LinearLayout.LayoutParams(0,-2,1));codeText=text("------",26);codeText.setTypeface(null,Typeface.BOLD);codeText.setTextColor(Color.rgb(76,201,255));receiveTop.addView(codeText);receiveCard.addView(receiveTop);
    LinearLayout pair=new LinearLayout(this);pair.setGravity(Gravity.CENTER);pair.setPadding(0,dp(12),0,dp(8));qr=new ImageView(this);qr.setBackgroundColor(Color.WHITE);pair.addView(qr,new LinearLayout.LayoutParams(dp(168),dp(168)));receiveCard.addView(pair);
    status=muted("Starting secure local receiver…",13);status.setGravity(Gravity.CENTER);receiveCard.addView(status);root.addView(receiveCard);
    LinearLayout pairingActions=new LinearLayout(this);pairingActions.setPadding(0,dp(2),0,dp(6));Button scan=iconButton("Scan QR",com.jepongdevxyz.phonetotv.R.drawable.ic_qr,false);Button enter=button("Enter code");scan.setOnClickListener(v->scanner.launch(new ScanOptions().setPrompt("Scan receiver QR").setBeepEnabled(false)));enter.setOnClickListener(v->promptCode());LinearLayout.LayoutParams half=new LinearLayout.LayoutParams(0,dp(58),1);half.setMargins(0,0,dp(5),0);pairingActions.addView(scan,half);LinearLayout.LayoutParams half2=new LinearLayout.LayoutParams(0,dp(58),1);half2.setMargins(dp(5),0,0,0);pairingActions.addView(enter,half2);root.addView(pairingActions);
    TextView nearTitle=text("Nearby devices",20);nearTitle.setTypeface(null,Typeface.BOLD);root.addView(nearTitle);root.addView(muted("Tap a device on the same Wi-Fi to connect.",13));peerBox=new LinearLayout(this);peerBox.setOrientation(LinearLayout.VERTICAL);root.addView(peerBox);
    LinearLayout filesCard=card();LinearLayout fileHead=new LinearLayout(this);fileHead.setGravity(Gravity.CENTER_VERTICAL);TextView ft=text("Files",18);ft.setTypeface(null,Typeface.BOLD);ft.setCompoundDrawablesWithIntrinsicBounds(com.jepongdevxyz.phonetotv.R.drawable.ic_files,0,0,0);ft.setCompoundDrawablePadding(dp(10));fileHead.addView(ft,new LinearLayout.LayoutParams(0,-2,1));pick=button("Add files");pick.setOnClickListener(v->choose());fileHead.addView(pick,new LinearLayout.LayoutParams(dp(130),dp(52)));filesCard.addView(fileHead);fileBox=new LinearLayout(this);fileBox.setOrientation(LinearLayout.VERTICAL);filesCard.addView(fileBox);root.addView(filesCard);
    progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(1000);LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,dp(7));pp.setMargins(0,dp(5),0,dp(12));root.addView(progress,pp);
    send=iconButton("Send files",com.jepongdevxyz.phonetotv.R.drawable.ic_send,true);send.setOnClickListener(v->beginSend());root.addView(send,new LinearLayout.LayoutParams(-1,dp(62)));
    LinearLayout actions=new LinearLayout(this);actions.setPadding(0,dp(10),0,0);cancel=button("Cancel");retry=button("Retry");cancel.setOnClickListener(v->{if(current!=null)current.cancel.set(true);});retry.setOnClickListener(v->beginSend());LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(0,dp(54),1);ap.setMargins(0,0,dp(5),0);actions.addView(cancel,ap);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,dp(54),1);rp.setMargins(dp(5),0,0,0);actions.addView(retry,rp);root.addView(actions);
    renderFiles();renderPeers();
  }
  void choose(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);i.addCategory(Intent.CATEGORY_OPENABLE);picker.launch(i);}
  void renderFiles(){fileBox.removeAllViews();fileBox.addView(text(files.isEmpty()?"No files selected":files.size()+" file(s) selected",15));for(Uri u:files)fileBox.addView(text(nameOf(u),14));}
  String nameOf(Uri u){String n=u.getLastPathSegment();try(var c=getContentResolver().query(u,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(c!=null&&c.moveToFirst())n=c.getString(0);}catch(Exception ignored){}return n==null?"file":n;}
  long sizeOf(Uri u){try(var c=getContentResolver().query(u,new String[]{OpenableColumns.SIZE},null,null,null)){if(c!=null&&c.moveToFirst())return c.getLong(0);}catch(Exception ignored){}return -1;}
  void startReceiver(){
    code=String.format(Locale.US,"%06d",new SecureRandom().nextInt(1000000));codeText.setText(code);server=new TransferServer();io.execute(server);
  }
  void receiverReady(int port){runOnUiThread(()->{status.setText("Receive mode ready • Same Wi‑Fi • Port "+port);makeQr("jepongfiles://"+localIp()+":"+port+"?code="+code);}); advertise(port); discover();}
  String localIp(){try{for(Enumeration<NetworkInterface> e=NetworkInterface.getNetworkInterfaces();e.hasMoreElements();)for(Enumeration<InetAddress>a=e.nextElement().getInetAddresses();a.hasMoreElements();){InetAddress x=a.nextElement();if(!x.isLoopbackAddress()&&x instanceof Inet4Address)return x.getHostAddress();}}catch(Exception ignored){}return "0.0.0.0";}
  void makeQr(String s){try{BitMatrix m=new MultiFormatWriter().encode(s,BarcodeFormat.QR_CODE,420,420);Bitmap b=Bitmap.createBitmap(420,420,Bitmap.Config.RGB_565);for(int y=0;y<420;y++)for(int x=0;x<420;x++)b.setPixel(x,y,m.get(x,y)?Color.BLACK:Color.WHITE);qr.setImageBitmap(b);}catch(Exception ignored){}}
  void advertise(int port){nsd=(NsdManager)getSystemService(NSD_SERVICE);NsdServiceInfo i=new NsdServiceInfo();i.setServiceName("Send Files-"+Build.MODEL);i.setServiceType(TYPE);i.setPort(port);try{i.setAttribute("code",code);}catch(Exception ignored){}registration=new NsdManager.RegistrationListener(){public void onRegistrationFailed(NsdServiceInfo s,int e){}public void onUnregistrationFailed(NsdServiceInfo s,int e){}public void onServiceRegistered(NsdServiceInfo s){}public void onServiceUnregistered(NsdServiceInfo s){}};try{nsd.registerService(i,NsdManager.PROTOCOL_DNS_SD,registration);}catch(Exception ignored){}}
  void discover(){discovery=new NsdManager.DiscoveryListener(){public void onDiscoveryStarted(String t){}public void onDiscoveryStopped(String t){}public void onStartDiscoveryFailed(String t,int e){}public void onStopDiscoveryFailed(String t,int e){}public void onServiceLost(NsdServiceInfo s){runOnUiThread(()->{for(Iterator<Peer> it=peers.iterator();it.hasNext();)if(it.next().name.equals(s.getServiceName()))it.remove();renderPeers();});}public void onServiceFound(NsdServiceInfo s){if(s.getServiceName().startsWith("Send Files-"))try{nsd.resolveService(s,new NsdManager.ResolveListener(){public void onResolveFailed(NsdServiceInfo x,int e){}public void onServiceResolved(NsdServiceInfo x){String c="";try{byte[] z=x.getAttributes().get("code");if(z!=null)c=new String(z);}catch(Exception ignored){}Peer p=new Peer(x.getServiceName(),x.getHost().getHostAddress(),x.getPort(),c);runOnUiThread(()->{boolean exists=false;for(Peer q:peers)if(q.host.equals(p.host)&&q.port==p.port){exists=true;break;}if(!exists){peers.add(p);renderPeers();}});}});}catch(Exception ignored){}}};try{nsd.discoverServices(TYPE,NsdManager.PROTOCOL_DNS_SD,discovery);}catch(Exception ignored){}}
  void promptCode(){final EditText e=new EditText(this);e.setInputType(2);e.setHint("6-digit code");new AlertDialog.Builder(this).setTitle("Pair with nearby device").setView(e).setPositiveButton("Pair",(d,w)->{String x=e.getText().toString().trim();for(Peer p:peers)if(x.equals(p.code)){selected=p;renderPeers();toast("Paired with "+p.name);return;}toast("No nearby device matches that code");}).setNegativeButton("Cancel",null).show();}
  void pairQr(String raw){try{Uri u=Uri.parse(raw);if(!"jepongfiles".equals(u.getScheme()))throw new Exception();String h=u.getHost();int p=u.getPort();String x=u.getQueryParameter("code");selected=new Peer("QR paired device",h,p,x);boolean exists=false;for(Peer q:peers)if(q.host.equals(h)&&q.port==p){exists=true;break;}if(!exists)peers.add(selected);renderPeers();toast("QR pairing complete");}catch(Exception e){toast("Invalid Send Files QR code");}}
  void renderPeers(){peerBox.removeAllViews();if(peers.isEmpty()){peerBox.addView(text("Searching on your Wi‑Fi…",14));return;}for(Peer p:peers){Button b=button((p==selected?"✓ ":"")+p.name+"  •  "+p.host);b.setOnClickListener(v->{selected=p;renderPeers();});peerBox.addView(b);}}
  void beginSend(){if(selected==null){toast("Select a nearby device first");return;}if(files.isEmpty()){toast("Choose one or more files first");return;}current=new TransferJob(selected,new ArrayList<>(files));io.execute(current);}
  void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
  class TransferJob implements Runnable{
    final Peer peer;final ArrayList<Uri> list;final AtomicBoolean cancel=new AtomicBoolean();TransferJob(Peer p,ArrayList<Uri>l){peer=p;list=l;}
    public void run(){long total=0;for(Uri u:list)total+=Math.max(0,sizeOf(u));long done=0;try{for(Uri u:list){if(cancel.get())throw new InterruptedIOException("Cancelled");String name=nameOf(u);long size=sizeOf(u);long offset=queryOffset(peer,name,size);done+=offset;sendOne(peer,u,name,size,offset,total,done);done+=Math.max(0,size-offset);}ui("Transfer complete",1000);}catch(Exception e){ui("Transfer stopped: "+e.getMessage(),progress.getProgress());}}
    long queryOffset(Peer p,String name,long size)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL("http://"+p.host+":"+p.port+"/offset?name="+URLEncoder.encode(name,"UTF-8")+"&size="+size).openConnection();c.setRequestProperty("X-Pair-Code",p.code);c.setConnectTimeout(5000);c.connect();if(c.getResponseCode()!=200)throw new IOException("Pairing rejected");try(BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream()))){return Long.parseLong(r.readLine());}}
    void sendOne(Peer p,Uri u,String name,long size,long offset,long total,long base)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL("http://"+p.host+":"+p.port+"/upload").openConnection();c.setDoOutput(true);c.setRequestMethod("POST");if(size>=offset)c.setFixedLengthStreamingMode(size-offset);c.setRequestProperty("X-Pair-Code",p.code);c.setRequestProperty("X-File-Name",URLEncoder.encode(name,"UTF-8"));c.setRequestProperty("X-File-Size",""+size);c.setRequestProperty("X-Offset",""+offset);c.connect();try(InputStream in=getContentResolver().openInputStream(u);OutputStream out=new BufferedOutputStream(c.getOutputStream())){long skipped=0;while(skipped<offset){long n=in.skip(offset-skipped);if(n<=0)break;skipped+=n;}byte[] buf=new byte[256*1024];long sent=0;for(int n;(n=in.read(buf))>0;){if(cancel.get())throw new InterruptedIOException("Cancelled");out.write(buf,0,n);sent+=n;int v=total<=0?0:(int)Math.min(1000,(base+sent)*1000/total);ui("Sending "+name+" • "+((offset+sent)*100/Math.max(1,size))+"%",v);}}if(c.getResponseCode()!=200)throw new IOException("Receiver error "+c.getResponseCode());}
  }
  String header(Map<String,String> h,String k,String d){String v=h.get(k);return v==null?d:v;}
  void ui(String s,int p){runOnUiThread(()->{status.setText(s);progress.setProgress(p);});}
  class TransferServer implements Runnable{
    ServerSocket ss;public void run(){try{ss=new ServerSocket(0);receiverReady(ss.getLocalPort());while(!ss.isClosed())io.execute(new Client(ss.accept()));}catch(Exception e){ui("Receiver stopped: "+e.getMessage(),0);}}
    class Client implements Runnable{final Socket s;Client(Socket x){this.s=x;}public void run(){try(s){s.setSoTimeout(15000);BufferedInputStream in=new BufferedInputStream(s.getInputStream());OutputStream out=s.getOutputStream();String first=line(in);if(first==null)return;Map<String,String>h=new HashMap<>();for(String l;(l=line(in))!=null&&!l.isEmpty();){int k=l.indexOf(':');if(k>0)h.put(l.substring(0,k).trim().toLowerCase(Locale.US),l.substring(k+1).trim());}if(!code.equals(h.get("x-pair-code"))){reply(out,403,"PAIR");return;}String path=first.split(" ")[1];if(path.startsWith("/offset")){String q=URLDecoder.decode(path.substring(path.indexOf("name=")+5).split("&")[0],"UTF-8");File f=dest(q);reply(out,200,""+(f.exists()?f.length():0));return;}if(path.equals("/upload")){String n=URLDecoder.decode(header(h,"x-file-name","file"),"UTF-8");long expected=Long.parseLong(header(h,"x-file-size","-1"));long off=Long.parseLong(header(h,"x-offset","0"));File f=dest(n);try(RandomAccessFile raf=new RandomAccessFile(f,"rw")){if(raf.length()!=off)raf.setLength(off);raf.seek(off);byte[]b=new byte[256*1024];long remain=expected<0?Long.MAX_VALUE:expected-off;while(remain>0){int z=in.read(b,0,(int)Math.min(b.length,remain));if(z<0)break;raf.write(b,0,z);remain-=z;}}reply(out,200,"OK");runOnUiThread(()->toast("Received: "+n));return;}reply(out,404,"NO");}catch(Exception ignored){}}}
    String line(InputStream in)throws IOException{ByteArrayOutputStream b=new ByteArrayOutputStream();int c;while((c=in.read())!=-1){if(c=='\n')break;if(c!='\r')b.write(c);if(b.size()>8192)throw new IOException("header");}return c==-1&&b.size()==0?null:b.toString("UTF-8");}
    void reply(OutputStream o,int c,String body)throws IOException{byte[]b=body.getBytes("UTF-8");o.write(("HTTP/1.1 "+c+" OK\r\nContent-Length: "+b.length+"\r\nConnection: close\r\n\r\n").getBytes("UTF-8"));o.write(b);o.flush();}
    File dest(String n){String safe=n.replaceAll("[\\\\/:*?\"<>|]","_");File d=new File(getExternalFilesDir(null),"Received");d.mkdirs();return new File(d,safe);}
  }
  static class Peer{String name,host,code;int port;Peer(String n,String h,int p,String c){name=n;host=h;port=p;code=c;}}
  @Override protected void onDestroy(){super.onDestroy();try{if(discovery!=null)nsd.stopServiceDiscovery(discovery);}catch(Exception ignored){}try{if(registration!=null)nsd.unregisterService(registration);}catch(Exception ignored){}try{if(server!=null&&server.ss!=null)server.ss.close();}catch(Exception ignored){}io.shutdownNow();}
}
