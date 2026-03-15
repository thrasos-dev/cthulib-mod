package net.pixeldreamstudios.cthulib.client.base;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.pixeldreamstudios.cthulib.client.DevModeManager;
import net.pixeldreamstudios.cthulib.client.UIElementPositionManager;
import net.pixeldreamstudios.cthulib.config.CthuLibConfig;

public abstract class DraggableTitleScreenWidget extends AbstractWidget {
    
    protected final UIElementPositionManager.ElementType elementType;
    protected float hoverAnimation = 0.0f;
    protected float pressAnimation = 0.0f;
    protected float pulseAnimation = 0.0f;
    protected DraggableTitleScreenWidget(int x, int y, int width, int height, 
                                        Component message, 
                                        UIElementPositionManager.ElementType elementType) {
        super(x, y, width, height, message);
        this.elementType = elementType;
    }
    protected boolean isDragging() {
        return elementType != null && UIElementPositionManager.isDragging(elementType);
    }

    protected void updateHoverAnimation(int mouseX, int mouseY) {
        CthuLibConfig cfg =
                CthuLibConfig.getInstance();
        if (!cfg.animationsEnabled) { hoverAnimation = this.isHovered() ? 1.0f : 0.0f; return; }
        float targetHover = this.isHovered() ? 1.0f : 0.0f;
        hoverAnimation = Mth.lerp(cfg.hoverAnimationSpeed, hoverAnimation, targetHover);
    }

    protected void updatePressAnimation() {
        CthuLibConfig cfg =
                CthuLibConfig.getInstance();
        if (!cfg.animationsEnabled) { pressAnimation = 0.0f; return; }
        pressAnimation = Mth.lerp(cfg.pressAnimationSpeed, pressAnimation, 0.0f);
    }

    protected void updatePulseAnimation(float speed) {
        CthuLibConfig cfg =
                CthuLibConfig.getInstance();
        if (!cfg.animationsEnabled) return;
        pulseAnimation += cfg.pulseAnimationSpeed;
    }
    protected void triggerPress() {
        pressAnimation = 1.0f;
    }
    protected float lerp(float amount, float from, float to) {
        return Mth.lerp(amount, from, to);
    }
    protected float easeInOutQuad(float t) {
        return t < 0.5f ? 2.0f * t * t : 1.0f - (float)Math.pow(-2.0f * t + 2.0f, 2.0f) / 2.0f;
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && elementType != null && DevModeManager.shouldAllowDragging() && this.isMouseOver(mouseX, mouseY)) {
            UIElementPositionManager.startDrag(elementType, mouseX, mouseY, this.getX(), this.getY());
            return true;
        }
        if (this.isMouseOver(mouseX, mouseY)) {
            return onWidgetClicked(mouseX, mouseY, button);
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDragging()) {
            UIElementPositionManager.PositionInfo pos = 
                UIElementPositionManager.updateDrag(elementType, mouseX, mouseY, this.width, this.height);
            if (pos != null) {
                this.setX(pos.x);
                this.setY(pos.y);
                onDragUpdate();
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isDragging()) {
            UIElementPositionManager.endDrag(elementType, getWidgetName());
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    protected abstract boolean onWidgetClicked(double mouseX, double mouseY, int button);
    protected void onDragUpdate() {
    }
    protected abstract String getWidgetName();
    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
