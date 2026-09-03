package com.skyblockextras.screen;

import com.skyblockextras.SkyblockExtrasClient;
import com.skyblockextras.config.SbeConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Unified drag-and-drop editor for all SBE HUD overlays. */
public class PositionEditorScreen extends Screen {
    private enum Element { PET, RNG }
    private final Screen parent;
    private Element selected;
    private boolean dragging;
    private double dragOffsetX, dragOffsetY;

    public PositionEditorScreen(Screen parent) { super(Component.literal("Skyblock Extras Position Editor")); this.parent = parent; }

    private SbeConfig config() { return SkyblockExtrasClient.CONFIG; }

    private int petW() { return Math.max(190, Math.round(190 * clamp(config().petScale, .5f, 3f))); }
    private int petH() { return Math.max(72, Math.round(72 * clamp(config().petScale, .5f, 3f))); }
    private int rngW() { return Math.max(240, Math.round(300 * clamp(config().rngDropOverlayScale, .5f, 3f))); }
    private int rngH() { return Math.round(82 * clamp(config().rngDropOverlayScale, .5f, 3f)); }
    private static float clamp(float v,float min,float max){return Math.max(min,Math.min(max,v));}

    private int x(Element e) {
        if(e==Element.PET)return Math.max(0,Math.min(width-petW(),config().petX));
        int w=rngW(); return config().rngDropOverlayX<0?(width-w)/2:Math.max(0,Math.min(width-w,config().rngDropOverlayX));
    }
    private int y(Element e) {
        if(e==Element.PET)return Math.max(0,Math.min(height-petH(),config().petY));
        int h=rngH(); return config().rngDropOverlayY<0?(height-h)/2:Math.max(0,Math.min(height-h,config().rngDropOverlayY));
    }
    private int w(Element e){return e==Element.PET?petW():rngW();}
    private int h(Element e){return e==Element.PET?petH():rngH();}

    private Element elementAt(double mx,double my){
        Element[] order=selected==null?new Element[]{Element.PET,Element.RNG}:new Element[]{selected,selected==Element.PET?Element.RNG:Element.PET};
        for(Element e:order){int ex=x(e),ey=y(e);if(mx>=ex&&mx<=ex+w(e)&&my>=ey&&my<=ey+h(e))return e;}
        return null;
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick){
        if(event.button()!=0)return super.mouseClicked(event,doubleClick);
        Element hit=elementAt(event.x(),event.y());
        if(hit!=null){
            if(doubleClick||selected==null||selected!=hit)selected=hit;
            dragging=true;dragOffsetX=event.x()-x(hit);dragOffsetY=event.y()-y(hit);return true;
        }
        return super.mouseClicked(event,doubleClick);
    }

    @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy){
        if(!dragging||selected==null||event.button()!=0)return super.mouseDragged(event,dx,dy);
        int nx=(int)Math.round(event.x()-dragOffsetX),ny=(int)Math.round(event.y()-dragOffsetY);
        if(selected==Element.PET){config().petX=Math.max(0,Math.min(width-petW(),nx));config().petY=Math.max(0,Math.min(height-petH(),ny));}
        else {config().rngDropOverlayX=Math.max(0,Math.min(width-rngW(),nx));config().rngDropOverlayY=Math.max(0,Math.min(height-rngH(),ny));}
        config().save();return true;
    }

    @Override public boolean mouseReleased(MouseButtonEvent event){if(event.button()==0&&dragging){dragging=false;config().save();return true;}return super.mouseReleased(event);}

    @Override public boolean mouseScrolled(double mouseX,double mouseY,double horizontalAmount,double verticalAmount){
        Element hit=elementAt(mouseX,mouseY);if(hit==null||verticalAmount==0)return super.mouseScrolled(mouseX,mouseY,horizontalAmount,verticalAmount);
        float step=verticalAmount>0?.10f:-.10f;
        if(hit==Element.PET)config().petScale=clamp(config().petScale+step,.5f,3f);
        else config().rngDropOverlayScale=clamp(config().rngDropOverlayScale+step,.5f,3f);
        selected=hit;config().save();return true;
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta){
        super.extractRenderState(g,mouseX,mouseY,delta);
        g.fill(0,0,width,height,0xB20A0A0D);

        // Skysoft-style instruction card.
        g.fill(4,4,326,132,0xEE120016);g.outline(4,4,322,128,0xFF8A42D2);
        g.text(font,"Skyblock Extras Position Editor",10,10,0xFFFF55FF,false);
        g.text(font,"Hover a HUD element to move it.",10,30,0xFFE0E0E5,false);
        g.text(font,"Double-click to select",10,46,0xFFFFFF55,false);
        g.text(font,"Left-click-drag to move",10,62,0xFFFFFF55,false);
        g.text(font,"Scroll to resize",10,78,0xFFFFFF55,false);
        g.text(font,"Esc to exit",10,94,0xFFFFFF55,false);
        g.text(font,"Selected: "+(selected==null?"None":selected==Element.PET?"Pet Overlay":"RNG Drop Overlay"),10,114,0xFFB86AF0,false);

        drawPet(g,mouseX,mouseY);drawRng(g,mouseX,mouseY);
        g.text(font,"Pet X: "+config().petX+" Y: "+config().petY+" Scale: "+String.format("%.1f",config().petScale),10,height-34,0xFF9B9CA8,false);
        g.text(font,"RNG X: "+config().rngDropOverlayX+" Y: "+config().rngDropOverlayY+" Scale: "+String.format("%.1f",config().rngDropOverlayScale),10,height-18,0xFF9B9CA8,false);
    }

    private void drawPet(GuiGraphicsExtractor g,int mx,int my){
        if(!config().petOverlayEnabled)return;
        int px=x(Element.PET),py=y(Element.PET),pw=petW(),ph=petH();boolean hover=inside(mx,my,px,py,pw,ph);
        int border=selected==Element.PET?0xFFE0A7FF:hover?0xFFC276FF:0xFF8A42D2;
        g.fill(px,py,px+pw,py+ph,selected==Element.PET?0xFF2A2030:0xDD202126);g.outline(px,py,pw,ph,border);
        g.text(font,"PET OVERLAY",px+12,py+10,0xFFE8D5F5,false);
        g.text(font,"[Lvl 169] Rose Dragon",px+12,py+28,0xFFFFAA00,false);
        g.text(font,"Level Progress: 87.7%",px+12,py+44,0xFF55FFFF,false);
        g.text(font,"Pet XP: 157,190,039",px+12,py+58,0xFF55FFFF,false);
        if(hover||selected==Element.PET)g.text(font,"Drag / scroll",px+12,py+ph-13,0xFFC276FF,false);
    }

    private void drawRng(GuiGraphicsExtractor g,int mx,int my){
        if(!config().rngDropOverlayEnabled)return;
        int px=x(Element.RNG),py=y(Element.RNG),pw=rngW(),ph=rngH();boolean hover=inside(mx,my,px,py,pw,ph);
        int border=selected==Element.RNG?0xFFFFA3FF:hover?0xFFFF55FF:0xFF8A42D2;
        if(config().rngDropOverlayBackgroundEnabled){g.fill(px,py,px+pw,py+ph,0xEE101018);g.outline(px,py,pw,ph,border);g.fill(px+1,py+1,px+pw-1,py+5,0xFFFF55FF);}
        g.text(font,"RNG DROP!  x2",px+(pw-font.width("RNG DROP!  x2"))/2,py+10,0xFFFF55FF,true);
        g.text(font,"Squash",px+(pw-font.width("Squash"))/2,py+31,0xFFFFFFFF,true);
        g.text(font,"Value: 1.25M",px+(pw-font.width("Value: 1.25M"))/2,py+52,0xFFFFD45A,true);
        if(hover||selected==Element.RNG)g.text(font,"Drag / scroll",px+12,py+ph-13,0xFFC276FF,false);
    }

    private boolean inside(double mx,double my,int x,int y,int w,int h){return mx>=x&&mx<=x+w&&my>=y&&my<=y+h;}
    @Override public void onClose(){Minecraft.getInstance().gui.setScreen(parent);}
}
