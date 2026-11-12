package com.pumpkings.coconpc.core.mob;

import com.pumpkings.coconpc.core.mob.model.ModelRig;
import com.pumpkings.coconpc.core.mob.model.RigAnimation;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class AnimationController {
    private final ModelRig rig;
    private final MobAnimationSet animationSet;
    private final Map<MobAnimationState, RigAnimation> animations;

    private MobAnimationState state = MobAnimationState.IDLE;
    private RigAnimation current;
    private float time;
    private boolean oneShotFinished;
    private boolean deathComplete;

    public AnimationController(ModelRig rig, MobDefinition definition) {
        this.rig = rig;
        this.animationSet = definition.getAnimationSet();

        Map<MobAnimationState, RigAnimation> loaded = new EnumMap<>(MobAnimationState.class);
        boolean additive = animationSet.isAdditiveRotation();
        for (MobAnimationState animationState : MobAnimationState.values()) {
            if (definition.getAnimation(animationState) == null) continue;
            loaded.put(animationState, rig.createAnimation(
                    definition.getAnimation(animationState), additive));
        }
        this.animations = Collections.unmodifiableMap(loaded);

        play(MobAnimationState.IDLE);
    }

    public void tick(float deltaSeconds, float horizontalSpeed) {
        float delta = Math.max(0f, Math.min(deltaSeconds, 0.25f));

        if (state == MobAnimationState.DEATH) {
            if (current == null) {
                deathComplete = true;
                return;
            }
            boolean finished = advance(delta, 1f);
            current.sample(time);
            if (finished) {
                deathComplete = true;
            }
            return;
        }

        if (state == MobAnimationState.ATTACK || state == MobAnimationState.HURT) {
            if (current == null || oneShotFinished) {
                transitionToLocomotion(horizontalSpeed);
                return;
            }

            boolean finished = advance(delta, 1f);
            current.sample(time);
            if (finished) {
                oneShotFinished = true;
            }
            return;
        }

        MobAnimationState nextState = locomotionState(horizontalSpeed);
        if (nextState != state) {
            play(nextState);
        }

        float playback = 1f;
        if (state == MobAnimationState.WALK) {
            playback = horizontalSpeed / animationSet.getWalkReferenceSpeed();
            playback = clamp(playback,
                    animationSet.getMinWalkPlayback(),
                    animationSet.getMaxWalkPlayback());
        }

        advance(delta, playback);
        if (current != null) {
            current.sample(time);
        }
    }

    private boolean advance(float deltaSeconds, float playbackSpeed) {
        if (current == null) return true;

        time += deltaSeconds * playbackSpeed;
        float length = current.getLength();

        if (isLooping(state)) {
            if (time >= length) {
                time %= length;
            }
            return false;
        }

        if (time >= length) {
            time = length;
            return true;
        }
        return false;
    }

    private boolean isLooping(MobAnimationState animationState) {
        return animationState == MobAnimationState.IDLE
                || animationState == MobAnimationState.WALK;
    }

    private MobAnimationState locomotionState(float horizontalSpeed) {
        if (horizontalSpeed < animationSet.getIdleThreshold()) {
            return MobAnimationState.IDLE;
        }
        return animations.containsKey(MobAnimationState.WALK)
                ? MobAnimationState.WALK
                : MobAnimationState.IDLE;
    }

    private void transitionToLocomotion(float horizontalSpeed) {
        play(locomotionState(horizontalSpeed));
    }

    private void play(MobAnimationState nextState) {
        state = nextState;
        current = animations.get(nextState);
        time = 0f;
        oneShotFinished = false;

        rig.resetPose();
        if (current != null) {
            current.sample(0f);
        }
    }

    public boolean triggerAttack() {
        if (state == MobAnimationState.DEATH || state == MobAnimationState.HURT) {
            return false;
        }
        if (state == MobAnimationState.ATTACK) {
            return false;
        }
        if (!animations.containsKey(MobAnimationState.ATTACK)) {
            return false;
        }

        play(MobAnimationState.ATTACK);
        return true;
    }

    public boolean triggerHurt() {
        if (state == MobAnimationState.DEATH) {
            return false;
        }
        if (!animations.containsKey(MobAnimationState.HURT)) {
            return false;
        }

        play(MobAnimationState.HURT);
        return true;
    }

    public boolean triggerDeath() {
        if (state == MobAnimationState.DEATH) {
            return false;
        }

        deathComplete = false;
        play(MobAnimationState.DEATH);
        if (current == null) {
            deathComplete = true;
            return false;
        }
        return true;
    }

    public boolean isDeathComplete() {
        return deathComplete;
    }

    public MobAnimationState getState() {
        return state;
    }

    public float getTime() {
        return time;
    }

    public String getCurrentAnimationName() {
        return current != null ? current.getName() : null;
    }

    public int getAnimationCount() {
        return animations.size();
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
