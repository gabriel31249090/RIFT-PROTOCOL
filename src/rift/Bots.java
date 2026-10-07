package rift;

import java.util.Random;
import static rift.Game.*;
import static rift.World.*;

/** Local bot perception and decisions. Aim/motion run every tick; sensing and paths do not. */
final class Bots {
    static final double FOV = Math.toRadians(110);
    static final double HALF_FOV_COS = Math.cos(FOV / 2);
    static final double[] AIM_SPEED = {Math.toRadians(105), Math.toRadians(180), Math.toRadians(265)};
    static final double[] THINK_INTERVAL = {.19, .14, .11};
    static final double[] REACTION = {.44, .29, .19};
    static final double MEMORY_SECONDS = 5;
    static final int PATHS_PER_FRAME = 2;

    enum Role {
        ENTRY("ENTRADA"), LURK("FLANCO"), SUPPORT("SUPORTE"), AWPER("PRECISÃO");
        final String label; Role(String label){this.label=label;}
    }
    enum Personality {
        AGGRESSIVE("AGRESSIVO"), CAREFUL("CAUTELOSO"), MARKSMAN("ATIRADOR");
        final String label; Personality(String label){this.label=label;}
    }
    enum Memory {
        NONE("VIGIANDO"), SEEN("ÚLTIMO CONTATO"), HEARD("SOM OUVIDO"), HURT("SOB FOGO");
        final String label; Memory(String label){this.label=label;}
    }
    enum Noise { STEP, SHOT, RELOAD, ABILITY, PLANT, LAND }

    /** Knowledge belongs to this observer, never to a shared omniscient target tracker. */
    static final class Mind {
        Role role = Role.SUPPORT;
        Personality personality = Personality.CAREFUL;
        Memory memory = Memory.NONE;
        double x, y, z, expires, nextThink, equip, actionTime, retreatUntil, nextCover;
        double coverX, coverZ, footstep, idleScan, patrolX, patrolZ, patrolUntil;
        int strafe = 1, guardSite;
        boolean aimHead, holding, hasCover;
        long noiseSequence;
    }

    private static final class Sound {
        long sequence;
        int team;
        double x, y, z, time, range;
        Noise kind;
    }

    final Game g;
    final Random random;
    private final Sound[] sounds = new Sound[64];
    long sequence, sightRays, paths;
    int pathBudget = PATHS_PER_FRAME;
    Site attackSite;

    Bots(Game game, long seed) {
        g = game;
        random = new Random(seed ^ 0x524946544149L);
        for (int i = 0; i < sounds.length; i++) sounds[i] = new Sound();
    }

    void beginRound() {
        attackSite = random.nextDouble() < .52 ? g.world.a : g.world.b;
        sequence = 0;
        for (Sound s : sounds) s.sequence = 0;
        for (Actor a : g.actors) {
            reset(a);
            Mind m = a.mind;
            m.personality = Personality.values()[Math.floorMod(a.id * 7 + (int) g.matchSeed, 3)];
            double roll = random.nextDouble();
            m.role = a.gun().kind.scoped() ? Role.AWPER
                : roll < (m.personality == Personality.AGGRESSIVE ? .50 : .24) ? Role.ENTRY
                : roll < .65 ? Role.SUPPORT : Role.LURK;
            if (a.carrier) m.role = Role.SUPPORT;
            m.guardSite = random.nextBoolean() ? 0 : 1;
            a.lane = m.role == Role.LURK ? 0 : 1;
        }
        // At least one defender holds each site; the remaining roles are weighted draws.
        int guards = 0;
        for (Actor a : g.actors) if (a.team != g.attackTeam && guards < 2) a.mind.guardSite = guards++;
    }

    void reset(Actor a) {
        Mind m = a.mind;
        m.memory = Memory.NONE;
        m.expires = m.equip = m.actionTime = m.retreatUntil = m.nextCover = 0;
        m.hasCover = m.holding = false;
        m.nextThink = g.time + a.id * .013;
        m.noiseSequence = sequence;
        m.patrolUntil = 0;
        m.footstep = a.id * .031;
        m.strafe = (a.id & 1) == 0 ? 1 : -1;
        a.target = null;
        a.perception = 0;
    }

    void beginFrame() { pathBudget = PATHS_PER_FRAME; }

    boolean inView(Actor a, Actor other) {
        double dx = other.x - a.x, dz = other.z - a.z;
        double distance = Math.hypot(dx, dz);
        double range = a.nearSight > 0 ? 7 : a.gun().kind.scoped() ? 72 : 54;
        if (distance > range || a.flash > 0 || a.detained > 0) return false;
        if (distance > .05 && (dx * Math.sin(a.yaw) + dz * Math.cos(a.yaw)) / distance < HALF_FOV_COS) return false;
        double elevation = Math.atan2(other.y + 1 - a.y - a.eyeHeight, Math.max(.01, distance));
        return Math.abs(elevation - a.pitch) < Math.toRadians(48);
    }

    boolean visible(Actor a, Actor other) {
        if (other.dead || other.team == a.team || !inView(a, other)) return false;
        sightRays++;
        return g.canSee(a, other);
    }

    void remember(Actor a, double x, double y, double z, Memory kind, double seconds) {
        Mind m = a.mind;
        m.x = x; m.y = y; m.z = z;
        m.memory = kind;
        m.expires = g.time + seconds;
    }

    void perceive(Actor a) {
        Mind m = a.mind;
        Actor best = null;
        double nearest = Double.POSITIVE_INFINITY;
        for (Actor enemy : g.actors) {
            if (enemy.team == a.team || enemy.dead) continue;
            double distance = a.distance(enemy) * (enemy == a.target ? .8 : 1);
            if (distance < nearest && visible(a, enemy)) { best = enemy; nearest = distance; }
        }
        if (best != a.target && best != null) {
            a.react = REACTION[g.settings.difficulty] + random.nextDouble() * .12;
            m.aimHead = random.nextDouble() < .08 + g.settings.difficulty * .075;
        }
        a.target = best;
        if (best != null) {
            double height = m.aimHead ? best.crouch ? 1.03 : 1.61 : best.crouch ? .72 : 1.0;
            remember(a, best.x, best.y + height, best.z, Memory.SEEN, MEMORY_SECONDS + g.settings.difficulty);
            if (a.team == g.player.team) best.revealed = Math.max(best.revealed, .25);
            m.noiseSequence = sequence;
        } else {
            hear(a);
            if (g.time >= m.expires) m.memory = Memory.NONE;
        }
        m.nextThink = g.time + THINK_INTERVAL[g.settings.difficulty];
    }

    /** Sound survives muted audio and contains only a noisy position at emission time. */
    void noise(Actor source, Noise kind, double range) {
        if (g.phase != Phase.LIVE) return;
        Sound s = sounds[(int) (++sequence % sounds.length)];
        s.sequence = sequence; s.team = source.team; s.kind = kind;
        s.x = source.x; s.y = source.y + 1; s.z = source.z;
        s.time = g.time; s.range = range;
    }

    void hear(Actor a) {
        Mind m = a.mind;
        Sound best = null;
        double loudest = 0;
        long first = Math.max(m.noiseSequence + 1, sequence - sounds.length + 1);
        for (long index = first; index <= sequence; index++) {
            Sound s = sounds[(int) (index % sounds.length)];
            if (s.sequence != index || s.team == a.team || g.time - s.time > 1 || a.flash > .8) continue;
            double distance = Math.hypot(s.x - a.x, s.z - a.z);
            double range = s.range * (.84 + g.settings.difficulty * .08);
            if (distance > range) continue;
            sightRays++;
            if (!g.world.visible(a.eye(), new V(s.x, s.y, s.z))) range *= .55;
            double strength = 1 - distance / range;
            if (strength > loudest) { best = s; loudest = strength; }
        }
        m.noiseSequence = sequence;
        if (best != null) {
            double error = best.kind == Noise.PLANT ? .3 : 1.3 - g.settings.difficulty * .35;
            remember(a, best.x + (random.nextDouble() - .5) * error,
                best.y, best.z + (random.nextDouble() - .5) * error, Memory.HEARD, 3.4);
        }
    }

    void hurt(Actor a, Actor from) {
        if (from == null || a.target != null || a == g.player && !g.observing) return;
        remember(a, from.x + random.nextDouble() - .5, from.y + 1,
            from.z + random.nextDouble() - .5, Memory.HURT, 2.5);
    }

    static double angle(double value) { return Math.atan2(Math.sin(value), Math.cos(value)); }

    void look(Actor a, double x, double y, double z, double dt) {
        double dx = x - a.x, dz = z - a.z;
        double yaw = angle(Math.atan2(dx, dz) - a.yaw);
        double pitch = Math.atan2(y - a.y - a.eyeHeight, Math.hypot(dx, dz)) - a.pitch;
        double length = Math.hypot(yaw, pitch);
        double fraction = length < 1e-8 ? 1 : Math.min(1, AIM_SPEED[g.settings.difficulty] * dt / length);
        a.yaw = angle(a.yaw + yaw * fraction);
        a.pitch = Settings.clamp(a.pitch + pitch * fraction, -1.35, 1.35);
    }

    void tick(Actor a, double dt) {
        Mind m = a.mind;
        a.react = Math.max(0, a.react - dt);
        a.skillCooldown -= dt;
        a.repath -= dt;
        m.equip = Math.max(0, m.equip - dt);
        if (g.time >= m.nextThink || a.target != null && a.target.dead) perceive(a);
        if (a.flash > 0 || a.detained > 0) a.target = null;
        Actor seen = a.target;
        double distance = m.memory == Memory.NONE ? 100 : Math.hypot(m.x - a.x, m.z - a.z);
        manageWeapon(a, distance);
        if (a.hp <= (m.personality == Personality.CAREFUL ? 40 : 27) || a.gun().reload > 0) {
            if (m.memory != Memory.NONE) m.retreatUntil = Math.max(m.retreatUntil, g.time + 1.1);
        }
        double ox = a.x, oz = a.z;
        boolean retreat = m.retreatUntil > g.time && m.memory != Memory.NONE;
        if (retreat) retreat(a, dt);
        else if (seen != null && !mustDefuse(a) && distance < (a.gun().kind.scoped() ? 55 : 28)) duel(a, dt);
        else navigate(a, dt);
        separate(a, dt);
        a.eyeHeight += ((a.crouch ? 1.05 : 1.63) - a.eyeHeight) * (1 - Math.exp(-dt * 14));
        a.moveSpeed = Math.hypot(a.x - ox, a.z - oz) / Math.max(.001, dt);
        a.walk += a.moveSpeed * dt;
        if (m.memory != Memory.NONE && !(seen == null && !retreat && !g.planted && distance < 1.5)) look(a, m.x, m.y, m.z, dt);
        m.footstep -= dt;
        if (a.moveSpeed > 3.3 && a.grounded && !a.crouch && m.footstep <= 0) { AudioSurface.step(g,a); noise(a, Noise.STEP, 19); m.footstep = .36; }
        if (seen != null) {
            fire(a);
            if (!seen.dead && a.skillCooldown <= 0 && a.emp <= 0 && a.detained <= 0 && a.react <= 0) {
                g.abilities.botCast(a, seen);
                a.skillCooldown = 8 + random.nextDouble() * 6;
            }
        }
    }

    void separate(Actor a, double dt) {
        if(a.objective>0||g.defuser==a)return;
        for(Actor other:g.actors)if(other!=a&&!other.dead&&Math.abs(a.y-other.y)<1.5){
            double dx=a.x-other.x,dz=a.z-other.z,distance=Math.hypot(dx,dz);
            if(distance>=.64)continue;
            if(distance<.001){dx=(a.id<other.id?-.5:.5);dz=.05;distance=Math.hypot(dx,dz);}
            double force=Math.min(.08,(.64-distance)*6*dt);
            g.move(a,dx/distance*force,dz/distance*force);
        }
    }

    void manageWeapon(Actor a, double distance) {
        Gun gun = a.gun();
        if (gun.ammo <= 0 && a.slot != 1 && distance < 14 && a.target != null && a.pistol.ammo > 0) {
            gun.reload = 0; gun.burstLeft = 0;
            a.slot = 1; a.mind.equip = .38;
            return;
        }
        if (a.slot == 1 && a.primary != null && (a.primary.ammo > 0 || a.target == null && g.time >= a.mind.expires && a.primary.reserve > 0) && (a.target == null || distance > 23)) {
            a.pistol.burstLeft = 0;
            a.slot = 2; a.mind.equip = a.primary.kind.scoped() ? .62 : .48;
            gun = a.primary;
        }
        if (gun.ammo == 0 || a.target == null && gun.ammo < Math.max(2, gun.kind.mag / 3)) g.reload(a);
    }

    void fire(Actor a) {
        Mind m = a.mind;
        Gun gun = a.gun();
        if (a.react > 0 || m.equip > 0 || a.detained > 0 || a.teleportTime > 0 || a.objective > 0 || g.defuser == a
            || gun.cooldown > 0 || gun.reload > 0 || gun.ammo <= 0) return;
        double desired = Math.atan2(m.x - a.x, m.z - a.z);
        double elevation = Math.atan2(m.y - a.y - a.eyeHeight, Math.hypot(m.x - a.x, m.z - a.z));
        if (Math.abs(angle(desired - a.yaw)) > .075 || Math.abs(elevation - a.pitch) > .06) return;
        // Revalidate when firing: a cached sighting never authorizes shooting through new cover/smoke.
        if (!visible(a, a.target)) { a.target = null; return; }
        if (g.settings.difficulty == 2 && a.moveSpeed > 1.6 && m.personality != Personality.AGGRESSIVE) return;
        gun.ammo--; a.invulnerable = 0; a.shotGlow = .06;
        double interval = gun.kind.interval;
        if (gun.kind.mode == FireMode.BURST) {
            if (gun.burstLeft == 0) gun.burstLeft = 3;
            gun.burstLeft--;
            if (gun.burstLeft == 0) interval = .40;
        }
        if (!gun.kind.sidearm() && !gun.kind.scoped() && gun.sprayStep >= 3 + g.settings.difficulty * 2) interval = .34;
        gun.cooldown = (interval + (2 - g.settings.difficulty) * .035) / (a.stim > 0 ? 1.25 : 1);
        double spread = shotSpread(a);
        double compensation = .28 + g.settings.difficulty * .26;
        double yaw = a.yaw + gun.yawRecoil * (1 - compensation);
        double pitch = a.pitch + gun.pitchRecoil * (1 - compensation);
        for (int pellet = 0; pellet < gun.kind.pellets; pellet++) {
            double sy = yaw + (random.nextDouble() - .5) * spread * 2;
            double sp = pitch + (random.nextDouble() - .5) * spread * 2;
            g.shoot(a, new V(Math.sin(sy) * Math.cos(sp), Math.sin(sp), Math.cos(sy) * Math.cos(sp)), gun.kind);
        }
        Combat.recoil(gun, a.crouch, false);
        noise(a, Noise.SHOT, gun.kind.silenced() ? 34 : 64);
        Actor listener = g.cameraActor();
        if (listener != null && listener.distance(a) < 40) {
            g.audio.playAt("distant", Math.sin(Math.atan2(a.x - listener.x, a.z - listener.z) - listener.yaw),
                Math.max(.08, 1 - listener.distance(a) / 45) * (g.world.visible(listener.eye(), a.eye()) ? 1 : .35));
        }
    }

    double shotSpread(Actor a) {
        return Combat.spread(a, a.gun().kind.scoped(), false) + .004 + (2 - g.settings.difficulty) * .005;
    }

    void duel(Actor a, double dt) {
        Mind m = a.mind;
        if (m.actionTime <= g.time) {
            m.actionTime = g.time + .32 + random.nextDouble() * .48;
            m.holding = random.nextDouble() < (a.gun().kind.scoped() ? .7 : .30 + g.settings.difficulty * .1);
            m.strafe = random.nextBoolean() ? 1 : -1;
            a.crouch = m.holding && random.nextDouble() < .20 + g.settings.difficulty * .16;
        }
        if (m.holding) return;
        a.crouch = false;
        double dx = m.x - a.x, dz = m.z - a.z, length = Math.max(.1, Math.hypot(dx, dz));
        double closing = length > 17 && m.personality == Personality.AGGRESSIVE ? .35 : length < 5 ? -.6 : 0;
        double speed = speed(a) * .80 * dt;
        double x = a.x, z = a.z;
        g.move(a, (dz * m.strafe + dx * closing) / length * speed,
            (-dx * m.strafe + dz * closing) / length * speed);
        if (Math.hypot(a.x - x, a.z - z) < speed * .2) m.strafe *= -1;
    }

    double speed(Actor a) {
        return (a.flash > 0 ? 1.2 : a.crouch ? 2.1 : 4.3) * (a.slow > 0 ? .4 : 1)
            * (a.detained > 0 ? .35 : 1) * (a.stim > 0 ? 1.25 : 1)
            * (a.tagTime > 0 ? .60 : 1) * a.gun().kind.mobility;
    }

    void retreat(Actor a, double dt) {
        Mind m = a.mind;
        a.crouch = false;
        if (g.time >= m.nextCover) {
            m.nextCover = g.time + 1;
            m.hasCover = false;
            double best = 13;
            V threat = new V(m.x, m.y, m.z);
            int checked = 0;
            for (Box box : g.world.solids) {
                if (box.y2() < 1.4 || a.x < box.x1() - 10 || a.x > box.x2() + 10 || a.z < box.z1() - 10 || a.z > box.z2() + 10) continue;
                for (int side = 0; side < 4 && checked < 28; side++) {
                    double x = side == 0 ? box.x1() - .7 : side == 1 ? box.x2() + .7 : Settings.clamp(a.x, box.x1(), box.x2());
                    double z = side == 2 ? box.z1() - .7 : side == 3 ? box.z2() + .7 : Settings.clamp(a.z, box.z1(), box.z2());
                    double distance = Math.hypot(x - a.x, z - a.z);
                    if (distance >= best || g.world.blocked(x, z, a.y, .34, 1.78)) continue;
                    checked++; sightRays++;
                    if (!g.world.visible(threat, new V(x, a.y + 1.3, z))) {
                        m.coverX = x; m.coverZ = z; best = distance; m.hasCover = true;
                    }
                }
            }
        }
        if (m.hasCover) travel(a, m.coverX, m.coverZ, dt);
        else {
            double dx = a.x - m.x, dz = a.z - m.z, length = Math.max(.1, Math.hypot(dx, dz));
            g.move(a, dx / length * speed(a) * dt, dz / length * speed(a) * dt);
        }
    }

    boolean mustDefuse(Actor a) { return g.planted && a.team != g.attackTeam && (g.spikeTime < 13 || a.target == null); }

    void navigate(Actor a, double dt) {
        Mind m = a.mind;
        a.crouch = false;
        double tx, tz;
        boolean urgent = g.planted && a.team != g.attackTeam;
        if (urgent) {
            if (g.defuser != null && g.defuser != a && !g.defuser.dead) {
                // The existing defuser keeps the channel; teammates spread to watch approaches.
                tx = g.spikeX + ((a.id & 1) == 0 ? -5 : 5);
                tz = g.spikeZ + 4;
                urgent = false;
            } else { tx = g.spikeX; tz = g.spikeZ; }
        }
        else if (a.carrier && !g.planted && g.world.site(a.x, a.z) != null && (a.target == null || a.distance(a.target) > 15)) return;
        else if (m.memory != Memory.NONE && !a.carrier && !g.planted) {
            tx = m.x; tz = m.z;
            if (Math.hypot(tx - a.x, tz - a.z) < 1.5) {
                double scan = Math.sin(g.time * 2 + a.id) * .85;
                double heading = Math.atan2(m.x - a.x, m.z - a.z) + scan;
                look(a, a.x + Math.sin(heading) * 5, a.y + a.eyeHeight, a.z + Math.cos(heading) * 5, dt);
                return;
            }
        } else if (g.abilities.pingLife > 0 && a.team == g.player.team && !a.carrier && !g.planted) {
            tx = g.abilities.pingPoint.x(); tz = g.abilities.pingPoint.z();
        } else if (g.flow.mode.respawn) {
            if (g.time > m.patrolUntil || Math.hypot(a.x - m.patrolX, a.z - m.patrolZ) < 2) {
                m.patrolUntil = g.time + 14;
                int cell = g.world.nearestCell(5 + random.nextDouble() * 134, 5 + random.nextDouble() * 118);
                m.patrolX = cell < 0 ? 72 : cell % World.NW + .5;
                m.patrolZ = cell < 0 ? 60 : cell / World.NW + .5;
            }
            tx = m.patrolX; tz = m.patrolZ;
        } else if (g.planted) {
            tx = g.spikeX + (a.id % 2 == 0 ? -5 : 5); tz = g.spikeZ + (a.id % 3 - 1) * 4;
        } else if (a.team == g.attackTeam) {
            boolean carrier = false;
            for (Actor other : g.actors) if (other.carrier && !other.dead) { carrier = true; break; }
            Site site = attackSite == null ? g.world.a : attackSite;
            if (!carrier) { tx = g.spikeX; tz = g.spikeZ; }
            else if (a.z > 84) { tx = site == g.world.a ? a.lane == 0 ? 6 : 23 : a.lane == 0 ? 137 : 117; tz = 78; }
            else if (a.z > 46) { tx = site == g.world.a ? a.lane == 0 ? 6 : 23 : a.lane == 0 ? 137 : 117; tz = 40; }
            else { tx = site.x() + (a.carrier ? 3 : (a.id % 3 - 1) * 4); tz = site.z() + (a.carrier ? 2 : 5); }
        } else {
            Site site = m.guardSite == 0 ? g.world.a : g.world.b;
            if (g.timer < 65) { tx = site == g.world.a ? 23 : 117; tz = g.timer < 35 ? 78 : 40; }
            else { tx = site.x() + (a.id % 3 == 0 ? 5 : -6); tz = site.z() + (a.id % 3 == 1 ? 5 : -7); }
        }
        if (urgent && Math.hypot(a.x - g.spikeX, a.z - g.spikeZ) < 2.15) return;
        travel(a, tx, tz, dt);
    }

    void travel(Actor a, double tx, double tz, double dt) {
        if ((a.repath <= 0 || Math.hypot(tx - a.destX, tz - a.destZ) > 3) && pathBudget > 0) {
            pathBudget--; paths++;
            a.path = g.world.path(a.x, a.z, tx, tz); a.pathIndex = 0;
            a.destX = tx; a.destZ = tz; a.repath = 1.35 + random.nextDouble() * .45;
        }
        if (a.pathIndex >= a.path.size()) {
            if (a.mind.memory == Memory.NONE) {
                double heading = (a.team == g.attackTeam ? Math.PI : 0) + Math.sin(g.time * .75 + a.id) * .8;
                look(a, a.x + Math.sin(heading) * 5, a.y + a.eyeHeight, a.z + Math.cos(heading) * 5, dt);
            }
            return;
        }
        V waypoint = a.path.get(a.pathIndex);
        double dx = waypoint.x() - a.x, dz = waypoint.z() - a.z, length = Math.hypot(dx, dz);
        if (length < .28) a.pathIndex++;
        else {
            double step = Math.min(speed(a) * dt, length);
            g.move(a, dx / length * step, dz / length * step);
            if (a.mind.memory == Memory.NONE) look(a, a.x + dx / length * 8, a.y + a.eyeHeight, a.z + dz / length * 8, dt);
        }
    }
}
