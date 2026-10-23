package com.monobogdan.simpletelegram;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.monobogdan.simpletelegram.transport.ClientManager;
import com.monobogdan.simpletelegram.transport.Packets;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(8,8,8), CARD = Color.rgb(22,22,22), RED = Color.rgb(255,38,59), WHITE = Color.WHITE, MUTED = Color.rgb(165,165,165);
    private LinearLayout content;
    private boolean adminEnabled = false;
    private void toast(final String s) { runOnUiThread(new Runnable(){ public void run(){ Toast.makeText(MainActivity.this, s, Toast.LENGTH_LONG).show(); }}); }
    private TextView label(String text, int size, int color) { TextView v = new TextView(this); v.setText(text); v.setTextSize(size); v.setTextColor(color); v.setPadding(0,6,0,6); return v; }
    private Button button(String text, View.OnClickListener click) { Button b = new Button(this); b.setText(text); b.setTextColor(WHITE); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(RED)); b.setOnClickListener(click); return b; }
    private void heading(String text) { TextView v=label(text,22,RED); v.setTypeface(null,Typeface.BOLD); v.setPadding(0,4,0,14); content.addView(v); }
    private void card(String title, String body, String action, View.OnClickListener click) {
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(14,10,14,12); box.setBackgroundColor(CARD);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,0,0,12); content.addView(box,lp);
        TextView t=label(title,18,WHITE); t.setTypeface(null,Typeface.BOLD); box.addView(t);
        if(body!=null && body.length()>0) box.addView(label(body,14,MUTED));
        if(action!=null) box.addView(button(action,click));
    }
    private JSONObject object(String key, String value) { JSONObject o=new JSONObject(); try{o.put(key,value);}catch(Exception ignored){} return o; }
    private void api(String path, final String success) { ClientManager.getCurrent().get(path, new ClientManager.Response(){public void onReady(String s){toast(success+"\n"+s);}}); }
    private void post(String path, JSONObject payload, final String success) { ClientManager.getCurrent().postJson(path,payload,new ClientManager.Response(){public void onReady(String s){toast(success+"\n"+s);}}); }
    private void showProfile() {
        content.removeAllViews(); heading("👤 ПРОФИЛЬ");
        card("@furry  👑 PREMIUM","+1 488  •  +380…\n⚡ Байты: —    💎 Коины: —\n🔴 Официальный модератор\nPremium включён локально на клиенте", "Обновить профиль", new View.OnClickListener(){public void onClick(View v){api("/user/me","Ответ /user/me:");}});
        card("Публикации • Подарки • Группы","Вкладки профиля",null,null);
        card("NFT-юзернеймы","Посмотреть доступные имена и купить ник.","Сменить юзернейм",new View.OnClickListener(){public void onClick(View v){showUsernames();}});
        card("🎨 Эмодзи и стикеры","Наборы кастомных эмодзи R1K1 / MVRDER.","Загрузить наборы",new View.OnClickListener(){public void onClick(View v){api("/emoji/packs","Наборы эмодзи:");}});
        card("🤖 R1K1 Father","Создание собственного бота.","Создать бота (/newbot)",new View.OnClickListener(){public void onClick(View v){showNewBot();}});
        TextView logo=findViewById(R.id.r1k1_logo); logo.setOnLongClickListener(new View.OnLongClickListener(){public boolean onLongClick(View v){showAdmin();return true;}});
    }
    private void showUsernames() {
        api("/usernames/shop","Магазин юзернеймов:");
        final EditText e=new EditText(this); e.setHint("Желаемый юзернейм");
        new AlertDialog.Builder(this).setTitle("Купить NFT-юзернейм").setView(e).setNegativeButton("Отмена",null).setPositiveButton("Купить",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){JSONObject o=object("username",e.getText().toString().replace("@",""));post("/usernames/buy",o,"Ответ покупки:");}}).show();
    }
    private void showNewBot() {
        final EditText e=new EditText(this); e.setHint("Имя бота");
        new AlertDialog.Builder(this).setTitle("R1K1 Father — /newbot").setMessage("Введите имя нового бота").setView(e).setNegativeButton("Отмена",null).setPositiveButton("Создать",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){post("/bots/create",object("name",e.getText().toString()),"Ответ создания бота:");}}).show();
    }
    private void showShop() {
        content.removeAllViews(); heading("🛒 МАГАЗИН");
        card("Обычные NFT","R1K1 • VOID • GHOST\nСтоимость и наличие загружаются с API.","Загрузить каталог",new View.OnClickListener(){public void onClick(View v){api("/shop","Каталог магазина:");}});
        card("🎁 Подарки","Роза • Сердце • Звезда","Открыть подарки",new View.OnClickListener(){public void onClick(View v){showGifts();}});
        card("⚒ Крафт","3 обычных NFT → 1 редкий NFT. Состав запроса нужно согласовать с API.","Крафт",new View.OnClickListener(){public void onClick(View v){post("/craft",new JSONObject(),"Ответ крафта:");}});
    }
    private void showGifts() {
        content.removeAllViews(); heading("🎁 ПОДАРКИ");
        String[] levels={"Обычный","Необычный","Редкий","Эпический","Легендарный","Уникальный","Мега","Ультра"};
        for(final String level:levels) card("🎁 "+level,"Подарки этого уровня загружаются с сервера.","Загрузить подарки",new View.OnClickListener(){public void onClick(View v){api("/gifts","Каталог подарков:");}});
        card("Отправить подарок","Для отправки требуется ID получателя и ID подарка из ответа API.","Отправить",new View.OnClickListener(){public void onClick(View v){showSendGift();}});
    }
    private void showSendGift() {
        LinearLayout form=new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); final EditText recipient=new EditText(this); recipient.setHint("ID получателя"); recipient.setInputType(InputType.TYPE_CLASS_TEXT); final EditText gift=new EditText(this); gift.setHint("ID подарка"); form.addView(recipient);form.addView(gift);
        new AlertDialog.Builder(this).setTitle("Отправить подарок").setView(form).setNegativeButton("Отмена",null).setPositiveButton("Отправить",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){JSONObject o=new JSONObject();try{o.put("recipient_id",recipient.getText().toString());o.put("gift_id",gift.getText().toString());}catch(Exception ignored){}post("/gifts/send",o,"Ответ отправки:");}}).show();
    }
    private void showChats() {
        content.removeAllViews(); heading("💬 ЧАТЫ");
        card("R1K1 Father","Создание ботов через /newbot.","Открыть",new View.OnClickListener(){public void onClick(View v){showNewBot();}});
        card("Розыгрыши Giveaway","Приз, участники, время окончания и победитель приходят с сервера.","Загрузить розыгрыши",new View.OnClickListener(){public void onClick(View v){api("/giveaways","Розыгрыши:");}});
        card("Чаты microtg","Загружаем список через существующий QueryChats API.","Обновить чаты",new View.OnClickListener(){public void onClick(View v){loadLegacyChats();}});
        loadLegacyChats();
    }
    private void loadLegacyChats() {
        ClientManager.getCurrent().queryChats(50,new ClientManager.Response(){public void onReady(final String raw){runOnUiThread(new Runnable(){public void run(){try{List<Packets.Chat> chats=Packets.parseChatListFromQueryResponse(raw);for(final Packets.Chat c:chats)card(c.Name,c.Text,"Открыть чат",new View.OnClickListener(){public void onClick(View v){Intent i=new Intent(MainActivity.this,DialogActivity.class);i.putExtra("id",c.ID);i.putExtra("name",c.Name);startActivity(i);}});}catch(Exception e){toast("API чатов пока не вернул совместимый ответ: "+e.getMessage());}}});}});
    }
    private void showAdmin() {
        final String[] items={"Создать NFT","Создать подарок","Запустить розыгрыш","Отмена"};
        new AlertDialog.Builder(this).setTitle("🔐 R1K1.SYS ADMIN").setItems(items,new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){if(which==3)return;final int selected=which;final EditText e=new EditText(MainActivity.this);e.setHint("JSON payload (например {\"name\":\"VOID\"})");new AlertDialog.Builder(MainActivity.this).setTitle(items[selected]).setView(e).setNegativeButton("Отмена",null).setPositiveButton("Отправить",new DialogInterface.OnClickListener(){public void onClick(DialogInterface di,int w){try{JSONObject payload=new JSONObject(e.getText().toString());String path=selected==0?"/admin/create_nft":selected==1?"/admin/create_gift":"/admin/giveaways/create";post(path,payload,"Ответ админ API:");}catch(Exception ex){toast("Нужен корректный JSON");}}}).show();}}).show();
    }
    @Override public void onCreate(Bundle state) { super.onCreate(state); setContentView(R.layout.activity_main); content=findViewById(R.id.content_panel);
        Configuration.getInstance(); Configuration.ApplicationConfiguration cfg=Configuration.getInstance().getApplicationConfiguration();
        ClientManager cm=ClientManager.getCurrent(); cm.setNodeAddress(cfg.nodeAddress);cm.setAuthorizationToken(cfg.sessionKey);
        cm.setErrorHandler(new ClientManager.NetworkErrorHandler(){public void onError(final String msg,int code){toast("API: "+msg);}});
        findViewById(R.id.tab_profile).setOnClickListener(new View.OnClickListener(){public void onClick(View v){showProfile();}});
        findViewById(R.id.tab_shop).setOnClickListener(new View.OnClickListener(){public void onClick(View v){showShop();}});
        findViewById(R.id.tab_gifts).setOnClickListener(new View.OnClickListener(){public void onClick(View v){showGifts();}});
        findViewById(R.id.tab_chats).setOnClickListener(new View.OnClickListener(){public void onClick(View v){showChats();}});
        showProfile();
    }
}
