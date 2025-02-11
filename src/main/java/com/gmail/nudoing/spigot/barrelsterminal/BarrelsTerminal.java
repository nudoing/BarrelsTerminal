package com.gmail.nudoing.spigot.barrelsterminal;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.*;
import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;

import java.util.*;


public final class BarrelsTerminal extends JavaPlugin implements Listener {

    //設定言語
    boolean japanese;
    // 樽の検索範囲
    Integer Barrel_search_range;

    //中身入りのたるを使用するか
    boolean use_filled_barrel;

    boolean option_remote;

    //文字列定数
    final String TERMINAL = "TERMINAL";
    final String MENU = "MENU";
    final String STORAGE = "@";
    final String FILTER = "FILTER";
    final String NONE = "";
    final String USED = "used";

    final String REMOTE = "REMOTE";

    private enum InvSide {
        OUTSIDE,
        BARREL,
        PLAYER
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(this,this);

        saveDefaultConfig();

        japanese = getConfig().getBoolean("japanese",false);
        Barrel_search_range = getConfig().getInt("range",20);
        use_filled_barrel = getConfig().getBoolean("use_filled_barrel",true);
        option_remote = getConfig().getBoolean("remote",false);

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic

    }

    //コマンド（ヘルプ用）
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String Label, @NotNull String[] args){

        if(command.getName().equalsIgnoreCase("BTHelp")){
            String sub;
            if(args.length == 0)
                sub = "1";
            else
                sub = args[0];

            if(japanese){
                switch (sub){
                    case "1":
                        sender.sendMessage("===========================================");
                        sender.sendMessage("たるの検索範囲は現在：" + Barrel_search_range + " マス になってます。(config.yml)");
                        sender.sendMessage("■基本操作■");
                        sender.sendMessage("空枠を左クリック：次のページ   空枠を右クリック：前のページ");
                        sender.sendMessage("枠外左クリック：新しいページ   枠外右クリック：ページを消す");
                        sender.sendMessage("つづき読むなら、/BTHelp 2");
                        break;
                    case "2":
                        sender.sendMessage("===========================================");
                        sender.sendMessage("■ターミナルたるの使い方■");
                        sender.sendMessage("TERMINAL 〇〇 って名前つけたたるは、ターミナルたる(便利)として使えます。");
                        sender.sendMessage("何も入れずに 左クリック or 枠外を 左クリック：メニューを開きます");
                        sender.sendMessage("何か入れて 左クリック：ストレージたるにアイテムをしまいます");
                        sender.sendMessage("右クリック：たる一覧を表示します。");
                        sender.sendMessage("枠外を 右クリック：メニューに登録されてない困ったるを開きます。");
                        sender.sendMessage("つづき読むなら、/BTHelp 3");
                        break;
                    case "3":
                        sender.sendMessage("===========================================");
                        sender.sendMessage("■メニューたるの使い方■");
                        sender.sendMessage("メニュー項目を編集するのは右クリックで。");
                        sender.sendMessage("項目を 左クリック で、ストレージたるを開きます。");
                        sender.sendMessage("項目を シフト＋左クリック で、フィルタを設定します。");
                        sender.sendMessage("項目とフィルタに使ったアイテムが、その項目に自動でしまわれます。");
                        sender.sendMessage("1ページ1たる使うため、結構なたるが必要になります。もっとたるを！");
                }

            }else{
                switch (sub){
                    case "1":
                        sender.sendMessage("Search range" + Barrel_search_range + " blocks (config.yml)");
                        sender.sendMessage("=== Basics ===");
                        sender.sendMessage("LMB:next page   RMB:prev page");
                        sender.sendMessage("LMB(outside):new page   RMB(outside):delete page");
                        sender.sendMessage("next help /BTHelp 2");
                        break;
                    case "2":
                        sender.sendMessage("=== TERMINAL ===");
                        sender.sendMessage("Name the barrel 'TERMINAL GroupName' ");
                        sender.sendMessage("LMB(empty TERMINAL): Open the MENU.");
                        sender.sendMessage("LMB(outside): Open the MENU.");
                        sender.sendMessage("LMB(not empty TERMINAL): Put items in storage");
                        sender.sendMessage("next help /BTHelp 3");
                        break;
                    case "3":
                        sender.sendMessage("=== MENU ===");
                        sender.sendMessage("RMB with item : Edit MENU");
                        sender.sendMessage("LMB on item : Open storage");
                        sender.sendMessage("SHIFT + LMB on item : Edit filter");
                        sender.sendMessage("=== What is \"page\" ===");
                        sender.sendMessage("\"page\" is named barrels. Need Many empty barrels.");
                }

            }


        }
        return false;
    }

    //クリックイベント
    @EventHandler
    public void onPlayerClickBarrel(@NotNull InventoryClickEvent e)
    {
//        getServer().broadcastMessage("Event");
//        if(e.getSlotType() == InventoryType.SlotType.CRAFTING){
//            getServer().broadcastMessage("クリック：クラフティング");
//            ItemStack i =e.getCurrentItem();
//            if(i!=null){
//                getServer().broadcastMessage(i.getType().toString());
//            }
//            CraftingInventory ci = (CraftingInventory)e.getInventory();
//
//            Recipe r = ci.getRecipe();
//            if(r != null)
//                getServer().broadcastMessage(ci.getRecipe().toString());
//            else
//                getServer().broadcastMessage("null");
//            return;
//        }

        //樽インベントリビューじゃなかったら、リモータルひらくかも。それいがいの場合はなにもしない。
        if(!(e.getInventory().getHolder() instanceof Barrel)) {
            if(option_remote){  //リモートオプションが有効時のみ
                if(e.getClick() == ClickType.SHIFT_LEFT) {
                    //シフトクリックされていたら、リモータルを開くことを試みる。
                    if(tryOpenRemoteBarrel((Player)e.getWhoClicked(),e.getCurrentItem())) e.setCancelled(true);
                }
            }
            return;
        }

        // 樽ゲット
        Barrel barrel = (Barrel) e.getInventory().getHolder();

        // 処理モードを設定
        switch (getBarrelType(barrel)){
            case TERMINAL:    // ターミナル樽
                func_terminal(e,barrel);
                break;
            case MENU:        // メニュー樽
                func_menu(e,barrel);
                break;
            case STORAGE:         //ストレージたる
            case FILTER:         //フィルターたる
                func_storage(e,barrel);
                break;
            case NONE:
                func_noNameBarrel(e,barrel);
                break;
            default:
                break;
        }
    }

    //ドラッグイベント（主にMENUの操作キャンセル用）
    @EventHandler
    public void onPlayerDragMenu(@NotNull InventoryDragEvent e){
        //樽インベントリビューじゃなかったら、何もしないで終わる。
        if(!(e.getInventory().getHolder() instanceof Barrel))
            return;

        // 樽ゲット
        Barrel barrel = (Barrel) e.getInventory().getHolder();

        // MENUたるだった場合、たる部分でドラッグされていたらキャンセルします。
        if(getBarrelType(barrel).equals(MENU)){
            Set<Integer> set = e.getRawSlots();
            set.removeIf(s -> (s > 27));
            if(!set.isEmpty()) e.setCancelled(true);
        }

    }


    //ターミナルたるがくりっくされたときの処理
    private void func_terminal(@NotNull InventoryClickEvent e,Barrel terminal){

        if(isEmpty(e.getCursor()) && !isEmpty(e.getCurrentItem()) && getInvSide(e) == InvSide.PLAYER
                && (e.getClick() == ClickType.SHIFT_RIGHT || e.getClick() == ClickType.MIDDLE || (e.getClick()==ClickType.NUMBER_KEY && e.getHotbarButton() == 0))){
            //マウスカーソルがカラ、カレントます目にアイテムが有り、プレイヤー側インベントリが中央クリックされた場合。
            // 検索開始
            ArrayList<Barrel> barrels = searchBarrels(terminal);

            //MENUで指定したしまい込みmapを取得
            HashMap<String,String> mapMaterialToStorageName = getMapMaterialToStorageName(barrels);

            String storageName = mapMaterialToStorageName.get(getName(e.getCurrentItem()));
            Player player = (Player)e.getWhoClicked();
            // カーソルがあってるアイテムをしまうべき箱を開ける
            openBarrel(player,get0Barrel(getNamedList(barrels,storageName)));
            e.setCancelled(true);
            return;
        }

        if(!isEmpty(e.getCursor()) || !isEmpty(e.getCurrentItem())){
            //マウスカーソルかカレントマス目にアイテムがあった場合、
            //ターミナル関連の処理はしません!
            return;
        }

        Player player = (Player)e.getWhoClicked();

        String groupName = getGroupName(terminal);
        // ターミナルにグループ名付いてなかったら……
        if(groupName.isEmpty()){
            setName(terminal,TERMINAL + " " + terminal.getX() + "," + terminal.getY() + "," + terminal.getZ());
            openBarrel(player,terminal);
            e.setCancelled(true);
            return;
        }

        if(isEmpty(terminal)){
            //ターミナルたる、からっぽ！
            if(e.getClick() == ClickType.LEFT){
                //左クリックされたら、メニュー樽があるかどうか探す。
                Barrel menu = searchMenuBarrel(terminal);

                if(!openBarrel(player,menu)){
                    //MENUが無かったら、つくって開く。
                    String menu_name = MENU + " " + groupName;
                    Barrel new_barrel = createNewPage(player,terminal,menu_name);
                    openBarrel(player,new_barrel);
                }

            }else if(e.getClick() == ClickType.RIGHT){
                //右クリされたら

                // 検索開始
                ArrayList<Barrel> barrels = searchBarrels(terminal);
                //外側クリックだった場合、使用していない、なかみからっぽのたるを消す。
                if(getInvSide(e) == InvSide.OUTSIDE){
                    for(Barrel b:barrels){
                        if(isEmpty(b)){
                            if(getViewers(b) == 0){
                                if(japanese)
                                    player.sendMessage("開放 " + b.getCustomName());
                                else
                                    player.sendMessage("Clean : " + b.getCustomName());
                                clearName(b);
                            }else{
                                if(japanese)
                                    player.sendMessage("使用中 " + b.getCustomName());
                                else
                                    player.sendMessage("In use : " + b.getCustomName());
                            }

                        }
                    }
                }


                ArrayList<Barrel> storageList;
                //右クリックされたら、所属ストレージたるを……
                if (getInvSide(e) == InvSide.OUTSIDE){
                    //外側クリックなら、MENU登録ないのだけ出す
                    player.sendMessage("=== No on the menu list ===");
                    storageList = getStorageListNoMenu(barrels);
                    if(!storageList.isEmpty()){
                        openBarrel(player,get0Barrel(storageList));
                    }else{
                        if(use_filled_barrel){
                            // 中身入りたるも使うフラグがONの場合、中身入りの名前無したるを探して開く。
                            if(openBarrel(player,searchFirstBarrel(terminal,null,true))){
                                player.sendMessage("Found a no name barrel");
                            }
                        }

                    }

                    ArrayList<String> str_barrel_name = new ArrayList<>();
                    for(Barrel barrel:storageList){
                        String s = barrel.getCustomName();
                        if(s != null && !s.isEmpty()){
                            str_barrel_name.add(s);
                        }
                    }
                    // 重複名を消す。
                    ArrayList<String> hash_set = new ArrayList<>(new HashSet<>(str_barrel_name));
                    for(String s:hash_set){
                        player.sendMessage(s);
                    }
                    player.sendMessage(hash_set.size() +" type  " + str_barrel_name.size() +" storage");

                }

            }
        }else{
            //ターミナルたる、中身入り！
            if(e.getClick() == ClickType.LEFT){
                //左クリックで、しまい込み作業を行う。

                // 検索開始
                ArrayList<Barrel> barrels = searchBarrels(terminal);

                // ターミナルに今入ってるもの
                terminal.update();
                ItemStack[] terminal_items = terminal.getInventory().getStorageContents();
                // それにマッチしたたるの名前をここに入れたい。
                String[] str_match_barrel_name = new String[terminal_items.length];

                //MENUで指定したしまい込みmapを取得
                HashMap<String,String> mapMaterialToStorageName = getMapMaterialToStorageName(barrels);
                // 所属たるのリストを取得
                ArrayList<Barrel> storageList = getTypeList(barrels,STORAGE);

                // どの名前のたるに入れるか、まず検索。
                for (int i = 0; i < terminal_items.length; i++) {
                    if (terminal_items[i] != null && terminal_items[i].getType() != Material.AIR) {
                        //カラじゃないスタックを処理する
                        str_match_barrel_name[i] = "";
                        // mapを検索
                        String storageName = mapMaterialToStorageName.get(getName(terminal_items[i]));
                        if(storageName != null){
                            //mapに値があったので、それをマッチネームに設定！
                            str_match_barrel_name[i] = storageName;
                        }
//                        else{
//                            //mapに値がない場合、周りのたるから探す……
//                            //入ってるたるを探す……
//                            for (Barrel target : storageList) {
//                                if (target.getInventory().contains(terminal_items[i].getType())) {
//                                    //入ってるたるを見つけた！
//                                    //名前をとっておく
//                                    str_match_barrel_name[i] = target.getCustomName();
//                                    break;
//                                }
//                            }
//                        }
                    }
                }

                //アイテムを入れていこう。
                for(Barrel target:storageList){
                    target.update();
                    String target_name = target.getCustomName();
                    Inventory target_inv = target.getInventory();

                    for(int i = 0; i < terminal_items.length; i++){
                        if(str_match_barrel_name[i] != null && !str_match_barrel_name[i].isEmpty() && str_match_barrel_name[i].equals(target_name) && terminal_items[i] != null && terminal_items[i].getType() != Material.AIR){
                            // 同じ名前なら、このたるに入れる！
                            HashMap<Integer,ItemStack> hash = target_inv.addItem(terminal_items[i]);
                            //のこりは戻す
                            terminal_items[i] = hash.get(0);
                        }
                    }
                    //このたるの処理は終わり
                }

                terminal.getInventory().setStorageContents(terminal_items);

                openBarrel(player,terminal);

                // OUTSIDEクリックで、はみでたたるがあった場合、たるをつくります。
                //ターミナルにまだアイテムが残っている場合……
                if(!isEmpty(terminal)){
                    //入り切らなかったたるの名前
                    ArrayList<String> str_overflow_barrel_name = getOverflowBarrelName(terminal_items, str_match_barrel_name);

                    //入り切らなかったたるがあったら、新しくたるを作る。
                    if(!str_overflow_barrel_name.isEmpty()){
                        // OUTSIDEをクリック。
                        ArrayList<String> hash_set = new ArrayList<>(new HashSet<>(str_overflow_barrel_name));
                        for(String name:hash_set){
                            createNewPage(player,terminal,name);
                        }
                    }

                    if(getInvSide(e) == InvSide.OUTSIDE){
                        //OUTSIDEクリックの場合は、
                        //邪魔なアイテムあってもとりあえずMENU開いちゃうぞって処理をする
                        openBarrel(player,get0Barrel(getTypeList(barrels,MENU)));
                    }else{
                        //OUTSIDE以外をクリック。なんらかの理由でアイテムがしまいきれない。
                        if(str_overflow_barrel_name.isEmpty()){
                            if(japanese)
                                player.sendMessage("どこに入れたらいいか、わかんないアイテムがあります。");
                            else
                                player.sendMessage("Contains undefined items.");
                        }

                    }
                }
            }


        }


    }


    @NotNull
    private static ArrayList<String> getOverflowBarrelName(ItemStack[] terminal_items, String[] str_match_barrel_name) {
        ArrayList<String> str_overflow_barrel_name = new ArrayList<>();
        for(int i = 0; i < terminal_items.length; i++){
            if(terminal_items[i] != null && terminal_items[i].getType() != Material.AIR){
                //はみでてるアイテムの入るべきたるの名前を確認。
                if( str_match_barrel_name[i] != null && !str_match_barrel_name[i].isEmpty())
                    str_overflow_barrel_name.add(str_match_barrel_name[i]);
            }
        }
        return str_overflow_barrel_name;
    }

    //メニューたるがクリックされたときの処理
    private void func_menu(@NotNull InventoryClickEvent e,Barrel barrel){

        boolean bMouseEmpty = isEmpty(e.getCursor());
        boolean bCurrentEmpty = isEmpty(e.getCurrentItem());

        Player player = (Player)e.getWhoClicked();

        //両方カラなら、MENUページ切り替え処理
        if(bMouseEmpty && bCurrentEmpty){
            //検索開始
            Barrel terminal = searchTerminalBarrel(barrel);
            ArrayList<Barrel> barrels = searchBarrels(terminal);

            switch (e.getClick()){
                case LEFT:
                    //つぎのたるを開く。ないときに枠外クリックだったら作る。
                    if(openNextBarrel(player,barrels,barrel)){
                        e.setCancelled(true);
                    }else if(getInvSide(e) == InvSide.OUTSIDE){
                        Barrel new_barrel = createNewPage(player,terminal,barrel.getCustomName());
                        openBarrel(player,new_barrel);
                        e.setCancelled(true);
                    }else{
                        if(japanese)
                            player.sendMessage("最後のMENUページです。 枠外 左クリック でページふやします。");
                        else
                            player.sendMessage("no next page :" + barrel.getCustomName());
                    }
                    break;
                case RIGHT:
                    if(getInvSide(e) == InvSide.OUTSIDE){
                        //枠外クリックだったら、たるのページを消す
                        if(clearPage(player,terminal,barrels,barrel))
                            e.setCancelled(true);
                    }else if(openPrevBarrel(player,barrels,barrel)) { //前のたるを開く。なかったらターミナルに戻る。
                        e.setCancelled(true);
                    }else{
                        if(openBarrel(player,terminal))
                            e.setCancelled(true);
                    }
                    break;
            }

        }

        // これ以降、たるの処理のみなので、たる以外クリックだったらそのまま終了してしまう。
        if(getInvSide(e) != InvSide.BARREL){
            switch (e.getClick()){
                case SHIFT_LEFT:
                case SHIFT_RIGHT:
                    e.setCancelled(true);
            }
            return;
        }

        //マウスがカラで、マスになにかある時 = メニューの項目に関する処理（ストレージ開く・フィルタを開く）
        if(bMouseEmpty && !bCurrentEmpty){
            //検索開始
            Barrel terminal = searchTerminalBarrel(barrel);
            ArrayList<Barrel> barrels = searchBarrels(terminal);

            if(e.getClick() == ClickType.LEFT){
                //左クリックすると、MENUの項目を開く処理
                String pageName = STORAGE + " " + getGroupName(barrel) + " " + getName(e.getCurrentItem());

                // 項目をひらく
                if(!openBarrel(player,get0Barrel(getNamedList(barrels,pageName)))){
                    // ひらけない場合、新しいたるを作る。
                    Barrel new_barrel = createNewPage(player,terminal,pageName);
                    openBarrel(player,new_barrel);
                }

            }else if(e.getClick() == ClickType.SHIFT_LEFT){
                //シフト左クリックなら、フィルタたるを開く処理。
                String pageName = FILTER + " " + getGroupName(barrel) + " " + getName(e.getCurrentItem());
                // 項目をひらく
                if(!openBarrel(player,get0Barrel(getNamedList(barrels,pageName)))){
                    // ひらけない場合、新しいたるを作る。
                    Barrel new_barrel = createNewPage(player,terminal,pageName);
                    openBarrel(player,new_barrel);
                }
            }

        }

        //両方アイテムがある時、通常の処理をキャンセル。
        if(!bMouseEmpty && !bCurrentEmpty){
            e.setCancelled(true);
        }

        //右クリ以外はそもそも禁止。
        if(e.getClick() != ClickType.RIGHT)
            e.setCancelled(true);


    }


    // ストレージたるがクリックされたときの処理
    private void func_storage(@NotNull InventoryClickEvent e,Barrel barrel){

        // クリックしたマス目かマウスカーソルになにかアイテムあったら、ストレージ関連の処理はしません。
        if(!isEmpty(e.getCursor()) || !isEmpty(e.getCurrentItem()))
            return;

        // グループに所属していないたるは、処理しません。
        String groupName = getGroupName(barrel);
        if(groupName.isEmpty())
            return;

        //クリックしたひと
        Player player = (Player) e.getWhoClicked();


        //たる検索前に、無関係の処理は返しちゃう
        if(e.getClick() != ClickType.LEFT && e.getClick() != ClickType.RIGHT)
            return;

        //検索開始
        Barrel terminal = searchTerminalBarrel(barrel);
        ArrayList<Barrel> barrels = searchBarrels(terminal);
        switch(e.getClick()){
            case LEFT:
                //つぎのたるを開く。ないときに枠外クリックだったら作る。
                if(openNextBarrel(player,barrels,barrel)){
                    e.setCancelled(true);
                }else if(getInvSide(e) == InvSide.OUTSIDE){
                    Barrel new_barrel = createNewPage(player,terminal,barrel.getCustomName());
                    openBarrel(player,new_barrel);
                    e.setCancelled(true);
                }else{
                    if(japanese)
                        player.sendMessage("次のたるはありません。 枠外 左クリック で作ります。" + barrel.getCustomName());
                    else
                        player.sendMessage("no next page :" + barrel.getCustomName());
                }
                break;
            case RIGHT:
                if(getInvSide(e) == InvSide.OUTSIDE){
                    //枠外クリックだったら、たるのページを消す
                    if(clearPage(player,terminal,barrels,barrel)){
                        e.setCancelled(true);
                    }
                }else if(openPrevBarrel(player,barrels,barrel)) { //前のたるを開く。なかったらメニュー1ページ目に戻る。
                    e.setCancelled(true);
                }else{
                    //メニューを開く（開けたら）
                    if(openBarrel(player,get0Barrel(getTypeList(barrels,MENU))))
                        e.setCancelled(true);
                }
                break;
        }

    }


    //ななしたるがクリックされたときの処理
    private void func_noNameBarrel(@NotNull InventoryClickEvent e,Barrel barrel){
        if(use_filled_barrel && e.getClick() == ClickType.SHIFT_RIGHT){
            //中身入りたるも空きたるとして使用するモードでマウスSHIFT_RIGHTだったら！
            if(!isEmpty(barrel) && isEmpty(e.getCursor())){
                //中身入り＆カーソルにアイテムなしだったら！
                ItemStack itemStack = e.getCurrentItem();
                if(!isEmpty(itemStack)){
                    //クリックしたアイテムがあったら！
                    //ターミナルゲット
                    Barrel terminal = searchNearestTerminal(barrel.getLocation());
                    // ターミナルのグループ名とアイテムスタックから、たる名をきめる。
                    String pageName = STORAGE + " " + getGroupName(terminal) + " " + getName(itemStack);

                    // 名前をつける
                    setName(barrel,pageName);
                    Player player = (Player) e.getWhoClicked();
                    openBarrel(player,barrel);
                    e.setCancelled(true);
                    if(japanese)
                        player.sendMessage("このたるに名前をつけました。 " + pageName);
                    else
                        player.sendMessage("Named :" + pageName);
                }
            }
        }
    }

    // たるが空かどうか確認する
    private boolean isEmpty(Barrel barrel) {
        Inventory inv = barrel.getInventory();
        for (ItemStack itemStack : inv) {
            //なかみがあるかな？
            if(itemStack != null && itemStack.getType() != Material.AIR)
                return false; //nullでも空気でもないitemStackがあったら、中身アリ！
        }
        return true;
    }

    //次のたるを開く
    private boolean openNextBarrel(@NotNull Player player,ArrayList<Barrel> barrels,Barrel barrel){
        ArrayList<Barrel> list = getNamedList(barrels,barrel.getCustomName());
        for(int i = 0; i<list.size();i++){

            if(list.get(i).equals(barrel)){
                if(i+1 < list.size()){
                    return openBarrel(player,list.get(i+1));
                }
            }
        }
        return false;
    }

    //前のたるを開く
    private boolean openPrevBarrel(@NotNull Player player,ArrayList<Barrel> barrels,Barrel barrel){
        ArrayList<Barrel> list = getNamedList(barrels,barrel.getCustomName());
        for(int i = 0; i<list.size();i++){

            if(list.get(i).equals(barrel)){
                if( 1 <= i ){
                    return openBarrel(player,list.get(i-1));
                }
            }
        }
        return false;
    }


    //リストとタイプを渡すと、そのタイプのリストをくれる。
    private ArrayList<Barrel> getTypeList(ArrayList<Barrel> barrels,String type){
        ArrayList<Barrel> result = new ArrayList<>();
        // ストレージをリザルトに追加
        for(Barrel b:barrels){
            if(getBarrelType(b).equals(type)){
                result.add(b);
            }
        }
        return result;
    }



    //リストを渡すと、MENUにないストレージのリストをくれる。
    private ArrayList<Barrel> getStorageListNoMenu(ArrayList<Barrel> barrels){
        // 所属たるのストレージ/フィルターのリストを取得
        ArrayList<Barrel> storageList = getTypeList(barrels,STORAGE);
        storageList.addAll(getTypeList(barrels,FILTER));

        // 入れるところmapを取得
        HashMap<String,String> map = getMenuItemListWithOutTypeName(barrels,MENU);
        // 重複しているストレージ名を消したものを取得。
        //Set<Material> set = map.keySet();

        ArrayList<String> list = new ArrayList<>(new HashSet<>(map.values()));

        ArrayList<String> hash_set = new ArrayList<>();

        for(String s:list){
            hash_set.add(STORAGE + s);
            hash_set.add(FILTER + s);
        }

        for(String s:hash_set){
            storageList.removeIf( b -> s.equals(b.getCustomName()));
        }
        return storageList;
    }



    //指定した名前のたるのリストを返す。
    @NotNull private ArrayList<Barrel> getNamedList(ArrayList<Barrel> barrels, String name){
        ArrayList<Barrel> result = new ArrayList<>();
        //名前がnullなら、カラ配列返しちゃう
        if(name == null || name.isEmpty()) return result;

        //同じ名前のをリザルトに入れてく
        for(Barrel b:barrels){
            if(name.equals(b.getCustomName())){
                result.add(b);
            }
        }
        return result;
    }

    private Barrel get0Barrel(ArrayList<Barrel> barrels){
        if(barrels.isEmpty()) return null;
        return barrels.get(0);
    }

    //getBarrelListFromTerminalと同じ順番で探索して、指定した名前のさいしょのたるを返す(name=nullを渡したら、最初の空きたるを返す）
    // (noEmptyOnly=trueの場合、空きたるチェック時に中身をチェックして中身が入ってるものを返す）
    private Barrel searchFirstBarrel(Barrel startBarrel, String name,boolean noEmptyOnly){
        if(startBarrel == null) return null;

        int centerX,centerZ,centerY;
        int minX,minZ,minY,maxX,maxZ,maxY;

        centerX = startBarrel.getLocation().getBlockX();
        centerZ = startBarrel.getLocation().getBlockZ();
        centerY = startBarrel.getLocation().getBlockY();

        World w  = startBarrel.getWorld();

        // 近い場所からrange範囲内までを走査する
        for(int distance = 0;distance <= Barrel_search_range;distance++){
            minY = centerY - distance;
            maxY = centerY + distance;
            for(int y = minY ; y <= maxY ; y++){
                minZ = centerZ - distance;
                maxZ = centerZ + distance;
                for(int z = minZ ; z <= maxZ ; z++){
                    minX = centerX - distance;
                    maxX = centerX + distance;
                    for(int x = minX ; x <= maxX ; x++){
                        // x,y,zのうちどれかが端っこなら樽をチェック！
                        if(x==minX || x==maxX || y==minY || y==maxY || z==minZ || z==maxZ){
                            Block blk = w.getBlockAt(x,y,z);
                            if(blk.getType() == Material.BARREL){
                                Barrel b = (Barrel) blk.getState();

                                if(name == null){
                                    //name=nullなら空きたるかどうかをチェック。空きたるならそれを返す。
                                    if(b.getCustomName() == null || b.getCustomName().isEmpty()){
                                        //ルートテーブルが設定されてなかったら、自由に使ってもいいよね？
                                        if(b.getLootTable() == null){
                                            boolean emp = isEmpty(b);
                                            if(noEmptyOnly){
                                                emp = !emp;
                                            }
                                            if(emp){
                                                //使えそうだぞ！
                                                return b;
                                            }
                                        }
                                    }
                                }else if(name.equals(b.getCustomName())){
                                    //指定名のたる見つけたらそれを返す
                                    return b;
                                }

                            }

                        }
                    }
                }
            }
        }
        return null;
    }


    //指定座標から一番近いターミナルを返します。
    private Barrel searchNearestTerminal(Location location){
        if(location == null) return null;

        int centerX,centerZ,centerY;
        int minX,minZ,minY,maxX,maxZ,maxY;

        centerX = location.getBlockX();
        centerZ = location.getBlockZ();
        centerY = location.getBlockY();

        World w = location.getWorld();
        if(w==null) return null;

        // 近い場所からrange範囲内までを走査する
        for(int distance = 0;distance <= Barrel_search_range;distance++){
            minY = centerY - distance;
            maxY = centerY + distance;
            for(int y = minY ; y <= maxY ; y++){
                minZ = centerZ - distance;
                maxZ = centerZ + distance;
                for(int z = minZ ; z <= maxZ ; z++){
                    minX = centerX - distance;
                    maxX = centerX + distance;
                    for(int x = minX ; x <= maxX ; x++){
                        // x,y,zのうちどれかが端っこなら樽をチェック！
                        if(x==minX || x==maxX || y==minY || y==maxY || z==minZ || z==maxZ){
                            Block blk = w.getBlockAt(x,y,z);
                            if(blk.getType() == Material.BARREL){
                                Barrel b = (Barrel) blk.getState();
                                if(TERMINAL.equals(getBarrelType(b))){
                                    return b;
                                }

                            }

                        }
                    }
                }
            }
        }
        return null;

    }


    // プレイヤーに強制的にたるを開かせる
    private boolean openBarrel(@NotNull Player player,Barrel barrel){
        //たるがなかったら帰る
        if(barrel ==null) return false;

        BukkitScheduler scheduler = getServer().getScheduler();
        scheduler.runTask(this, () -> player.openInventory(barrel.getInventory()));
        return true;
    }


    //アイテムスタック空かな？
    private boolean isEmpty(ItemStack itemStack){
        return (itemStack == null || itemStack.getType() == Material.AIR);
    }

    //とくていのたるグループに、新しくなまえつきのたるをつくる
    private Barrel createNewPage(@NotNull Player player, Barrel terminal, String name){
        if(name == null || name.isEmpty())
            return null;

        if(terminal == null) return null;   //ターミナルなし。

        Barrel new_barrel = searchFirstBarrel(terminal,null,false);
        if(new_barrel !=null){
            setName(new_barrel,name);
            if(japanese)
                player.sendMessage("新しいたるを準備しました。 " + name);
            else
                player.sendMessage("Named :" + name);
        }else{
            if(japanese)
                player.sendMessage("空いてるたるがありません。もっとたるを！ " + name);
            else
                player.sendMessage("Need more barrels : for " + name);
        }
        return new_barrel;
    }

    //たるになまえつける
    private void setName(Barrel barrel,String name){
        barrel.setCustomName(name);
        barrel.update();
    }

    //たるのなまえけす
    private void clearName(Barrel barrel){
        setName(barrel,null);
    }

    private int getViewers(Barrel barrel){
        return barrel.getInventory().getViewers().size();
    }


    //たるのページをけす
    private boolean clearPage(Player player,Barrel terminal,ArrayList<Barrel> barrels,Barrel barrel){
        //たるの中身をかくにん
        if(isEmpty(barrel)){

            // ビュワーが一人以下（自分のみ）なら消しても問題ない。
            if(getViewers(barrel) <= 1){
                if(japanese)
                    player.sendMessage("たるを野良に解き放ちました。" + barrel.getCustomName());
                else
                    player.sendMessage("Erased : " + barrel.getCustomName());

                //ひとつ前のたるをあける
                if(!openPrevBarrel(player,barrels,barrel)){
                    //なければ、ターミナルを開く
                    openBarrel(player,terminal);
                }
                clearName(barrel);
                return true;
            }else{
                if(japanese)
                    player.sendMessage("このたるは誰かが使用中です。" + barrel.getCustomName());
                else
                    player.sendMessage("Can't erased. In use. : " + barrel.getCustomName());
            }

        }else{
            if(japanese)
                player.sendMessage("先にたるをからっぽにしてください。");
            else
                player.sendMessage("Please empty before that.");
        }
        return false;
    }

    //指定したたるの属する、ターミナルたるをぬきだす。
    private Barrel searchTerminalBarrel(@NotNull Barrel barrel){
        //グループ名
        String groupName = getGroupName(barrel);
        //なしならnull返す
        if(groupName.isEmpty()) return null;

        //ターミナルたるの名前
        String terminalName = TERMINAL + " " + groupName;
        //barrel自体がターミナルなら、そのまま帰す
        if(terminalName.equals(barrel.getCustomName())){
            return barrel;
        }

        // さいしょにみつけたターミナルたるを返す。（見つからなければnull）
        return searchFirstBarrel(barrel,terminalName,false);
    }

    //近くのメニューたるを返す。
    private Barrel searchMenuBarrel(@NotNull Barrel terminal){
        //グループ名
        String groupName = getGroupName(terminal);
        //なしならnull返す
        if(groupName.isEmpty()) return null;
        //メニューたるの名前
        String menuName = MENU + " " + groupName;
        //さいしょにみつけたメニューたるを返す。（見つからなければnull）
        return searchFirstBarrel(terminal,menuName,false);
    }

    // terminalから近い順で、同じグループに属するたるのリストを返す。
    private ArrayList<Barrel> searchBarrels(Barrel terminal){
        //返り値用リスト
        ArrayList<Barrel> barrelList = new ArrayList<>();

        // terminalがそもそもnullならカラで返す
        if(terminal == null) return barrelList;

        // グループ名ゲット
        String groupName = getGroupName(terminal);
        // グループ名なしなら帰る。
        if(groupName.isEmpty()) return barrelList;

        int centerX,centerZ,centerY;
        int minX,minZ,minY,maxX,maxZ,maxY;

        centerX = terminal.getLocation().getBlockX();
        centerZ = terminal.getLocation().getBlockZ();
        centerY = terminal.getLocation().getBlockY();

        World w  = terminal.getWorld();

        // 近い場所からrange範囲内までを走査する
        for(int distance = 1;distance <= Barrel_search_range;distance++){
            minY = centerY - distance;
            maxY = centerY + distance;
            for(int y = minY ; y <= maxY ; y++){
                minZ = centerZ - distance;
                maxZ = centerZ + distance;
                for(int z = minZ ; z <= maxZ ; z++){
                    minX = centerX - distance;
                    maxX = centerX + distance;
                    for(int x = minX ; x <= maxX ; x++){
                        // x,y,zのうちどれかが端っこなら樽をチェック！
                        if(x==minX || x==maxX || y==minY || y==maxY || z==minZ || z==maxZ){
                            Block blk = w.getBlockAt(x,y,z);
                            if(blk.getType() == Material.BARREL){
                                Barrel b = (Barrel) blk.getState();
                                if(groupName.equals(getGroupName(b))){
                                    barrelList.add(b);
                                }
                            }

                        }
                    }
                }
            }
        }
        return barrelList;
    }


    // たるからグループ名をぬきだす。(なければ""を返す。)
    private String getGroupName(Barrel barrel){
        // 名無したるならリストなし。
        if(barrel == null || barrel.getCustomName() == null || barrel.getCustomName().isEmpty())
            return "";
        String[] args = barrel.getCustomName().split(" ");
        if(args.length <= 1)
            return "";
        return args[1];
    }


    //クリックされた領域を確認
    private InvSide getInvSide(@NotNull InventoryClickEvent e) {
        Inventory clickedInv = e.getClickedInventory();
        if(clickedInv == null){
            return InvSide.OUTSIDE;
        }else if(clickedInv.getHolder() instanceof Barrel) {
            return InvSide.BARREL;
        }else{
            return InvSide.PLAYER;
        }
    }

    //メニューなどをチェックして、どのアイテムをどこに入れればいいかの一覧を返す。
    private HashMap<String,String> getMapMaterialToStorageName(ArrayList<Barrel> barrels){

        //FILTERのアイテムを調査
        HashMap<String,String> map = getMenuItemListWithOutTypeName(barrels,FILTER);
        //MENUのアイテムを調査
        map.putAll(getMenuItemListWithOutTypeName(barrels,MENU));

        Set<String> set = map.keySet();
        for(String s:set){
            map.put(s,STORAGE + map.get(s));
        }
        return map;
    }

    /**
     *
     * @param barrels 処理対象のたる
     * @param barrelType 処理するたるたいぷ（MENU\FILTER)
     * @return <アイテム名,入れる項目名> のmap
     */
    private HashMap<String,String> getMenuItemListWithOutTypeName(ArrayList<Barrel> barrels,String barrelType){
        HashMap<String,String> map = new HashMap<>();

        //MENUに設定したアイテムの入れるところ
        ArrayList<Barrel> listMenu = getTypeList(barrels,barrelType);
        String groupName = getGroupName(get0Barrel(listMenu));
        for(Barrel b:listMenu){
            ItemStack[] itemStacks = b.getInventory().getStorageContents();
            for(ItemStack i:itemStacks){
                if(i != null && i.getType() != Material.AIR){
                    switch (barrelType){
                        case MENU:
                            String s = " " + groupName + " " + getName(i);
                            map.put(getName(i),s);
                            break;
                        case FILTER:
                            //FILTERな事を確認済なので、ぬるぽしない。
                            String[] ss = Objects.requireNonNull(b.getCustomName()).split(" ");
                            ss[0] = "";
                            String name = String.join(" ",ss);
                            map.put(getName(i),name);
                            break;
                    }
                }
            }
        }

        return map;
    }


    //ItemStackのDisplayNameを取得（なければMaterialTypeNameを返す）
    private String getName(ItemStack itemStack){
        if(!itemStack.hasItemMeta()) return itemStack.getType().name();
        ItemMeta meta = itemStack.getItemMeta();
        if(meta == null) return itemStack.getType().name();
        if(!meta.hasDisplayName()) return itemStack.getType().name();
        return meta.getDisplayName();
    }


    //たるの種類を返す。
    private String getBarrelType(Barrel barrel){
        // 名無したるならリストなし。
        if(barrel.getCustomName() == null || barrel.getCustomName().isEmpty())
            return NONE;
        String[] args = barrel.getCustomName().split(" ");

        switch (args[0]){
            case TERMINAL:
            case MENU:
            case STORAGE:
            case FILTER:
                return args[0];
            default:
                return USED;
        }
    }

    private boolean tryOpenRemoteBarrel(@NotNull Player player,ItemStack itemStack){

        if(itemStack == null) return false;
        if(itemStack.getType() != Material.BARREL) return false;
        ItemMeta itemMeta = itemStack.getItemMeta();
        if(itemMeta == null) return false;
        String[] args = itemMeta.getDisplayName().split(" ");
        if(! args[0].equals(REMOTE)) return false;


        Barrel barrel = getRemoteBarrel(player,itemMeta);
        if(barrel == null){
            barrel = searchNearestTerminal(player.getLocation());
            if(barrel != null){
                List<String> lore = new ArrayList<>();
                int x =barrel.getLocation().getBlockX();
                int y =barrel.getLocation().getBlockY();
                int z =barrel.getLocation().getBlockZ();
                lore.add(x +","+ y +","+ z);
                itemMeta.setLore(lore);

                String groupName = getGroupName(barrel);
                itemMeta.setDisplayName(args[0]+" "+groupName);
                itemStack.setItemMeta(itemMeta);
            }
        }


        openBarrel(player,barrel);

        return true;
    }

    private Barrel getRemoteBarrel(@NotNull Player player,ItemMeta itemMeta){
        List<String> lore =  itemMeta.getLore();
        if(lore == null) return null;
        String lore0 = lore.get(0);
        String[] loc = lore0.split(",");

        if(loc.length !=3) return null;
        int x = Integer.parseInt(loc[0]);
        int y = Integer.parseInt(loc[1]);
        int z = Integer.parseInt(loc[2]);

        World world = player.getLocation().getWorld();
        if(world == null) return null;

        Block block = world.getBlockAt(x,y,z);
        if(block.getType() != Material.BARREL) return null;
        return (Barrel) block.getState();
    }

}
