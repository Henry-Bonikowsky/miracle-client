# Fabric Mixin & Screen Rendering: Complete Expert Guide

**Date:** January 2, 2026
**Purpose:** Comprehensive guide to understanding Fabric mixins and Minecraft screen rendering
**Sources:** 30+ authoritative references

---

## Table of Contents
1. [What Are Mixins?](#1-what-are-mixins)
2. [Core Mixin Annotations](#2-core-mixin-annotations)
3. [Injection Points (@At)](#3-injection-points-at)
4. [Screen Class Architecture](#4-screen-class-architecture)
5. [Rendering System (DrawContext)](#5-rendering-system-drawcontext)
6. [Common Mixin Patterns](#6-common-mixin-patterns)
7. [Troubleshooting Mixin Errors](#7-troubleshooting-mixin-errors)
8. [Multi-Version Support](#8-multi-version-support)
9. [Best Practices](#9-best-practices)
10. [Quick Reference Cheat Sheet](#10-quick-reference-cheat-sheet)

---

## 1. What Are Mixins?

### The Basics
Mixins are a **bytecode weaving framework** that allows you to modify Minecraft's code without directly editing it. Unlike other modding approaches, Fabric Loader doesn't overwrite Minecraft's class files - instead, code is **injected at runtime** using the Mixin library.

**Key Concept:** When the game loads, the Mixin processor reads your mixin classes and *weaves* your code into the target classes at the bytecode level using the ASM library.

### How It Works
```
Your Mixin Class → Mixin Processor → ASM Bytecode Manipulation → Modified Target Class
```

The Mixin Annotation Processor generates a **Reference Map (refmap)** at compile time. This file maps your human-readable method names to their obfuscated equivalents, allowing your mod to work in production environments.

### Why Use Mixins?
- **Non-destructive:** Original code remains intact
- **Chainable:** Multiple mods can modify the same method
- **Flexible:** Target any point in any method
- **Compatible:** Works across Minecraft versions with proper mappings

---

## 2. Core Mixin Annotations

### @Mixin - Declaring a Mixin
```java
@Mixin(TitleScreen.class)
public class TitleScreenMixin {
    // Your modifications here
}
```
The `@Mixin` annotation marks a class as a mixin targeting the specified class.

### @Inject - Adding Code
```java
@Inject(method = "init", at = @At("HEAD"))
private void onInit(CallbackInfo ci) {
    // Runs at the start of init()
}
```
**All inject methods MUST accept a `CallbackInfo` (or `CallbackInfoReturnable<T>` for methods with return values) as their last parameter.**

### @Shadow - Accessing Target Fields/Methods
```java
@Shadow
private MinecraftClient client;

@Shadow
protected abstract void doSomething();
```
Creates a "virtual" reference to a field or method in the target class.

**Important:** If shadowing a `final` field, add `@Final` above `@Shadow`. If you need to modify it, also add `@Mutable`.

### @Accessor - External Field Access
```java
@Mixin(SomeClass.class)
public interface SomeClassAccessor {
    @Accessor("privateField")
    int getPrivateField();

    @Accessor("privateField")
    void setPrivateField(int value);
}
```
Generates getter/setter methods for private fields. Must be defined in an **interface mixin**.

### @Invoker - External Method Access
```java
@Mixin(SomeClass.class)
public interface SomeClassInvoker {
    @Invoker("privateMethod")
    void callPrivateMethod();
}
```
Generates a method to call private methods. Also requires an interface mixin.

### @Unique - Adding New Members
```java
@Unique
private int myMod$customField;

@Unique
private void myMod$helperMethod() { }
```
**Always prefix custom fields/methods with `modid$`** to avoid conflicts with other mods.

---

## 3. Injection Points (@At)

The `@At` annotation specifies WHERE in a method to inject code.

### HEAD
```java
@At("HEAD")
```
Injects at the **very first instruction** of the method.

### TAIL
```java
@At("TAIL")
```
Injects **just before the final RETURN** statement (not middle returns).

### RETURN
```java
@At("RETURN")
```
Injects before **ALL return statements** including early returns.

### INVOKE
```java
@At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;setScreen(Lnet/minecraft/client/gui/screen/Screen;)V")
```
Injects before a specific **method call** within the target method.

### INVOKE_ASSIGN
```java
@At(value = "INVOKE_ASSIGN", target = "...")
```
Injects **after** a method call, when the result is assigned.

### FIELD
```java
@At(value = "FIELD", target = "Lsome/Class;fieldName:I", opcode = Opcodes.GETFIELD)
```
Injects at field access (GET or PUT).

### NEW
```java
@At(value = "NEW", target = "Ljava/util/ArrayList;")
```
Injects before object instantiation.

### Ordinal Parameter
```java
@At(value = "INVOKE", target = "...", ordinal = 0)
```
When multiple matches exist, `ordinal` specifies which one (0-indexed).

### Shift Parameter
```java
@At(value = "INVOKE", target = "...", shift = At.Shift.AFTER)
```
- `NONE` - Default, inject at the exact point
- `AFTER` - Inject after the target instruction
- `BEFORE` - Inject before (same as NONE for most)

---

## 4. Screen Class Architecture

### Class Hierarchy
```
AbstractParentElement
    └── Screen (abstract)
            ├── TitleScreen
            ├── OptionsScreen (net.minecraft.client.gui.screen.option)
            ├── MultiplayerScreen (net.minecraft.client.gui.screen.multiplayer)
            ├── SelectWorldScreen
            ├── GameMenuScreen
            └── [Many others...]
```

### Key Screen Methods

| Method | Purpose | Override? |
|--------|---------|-----------|
| `init()` | Set up widgets, called on resize | Yes |
| `render()` | Draw the screen | Optional |
| `renderBackground()` | Draw background | Rarely |
| `tick()` | Per-tick updates | Optional |
| `close()` | Screen closing | Optional |
| `shouldPause()` | Pause game when open? | Yes |

### Creating Custom Screens
```java
public class MyScreen extends Screen {
    public MyScreen() {
        super(Text.literal("My Screen"));
    }

    @Override
    protected void init() {
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Click Me"),
            button -> { /* action */ }
        ).dimensions(width/2 - 100, height/2, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);  // MUST call super first!
        // Custom rendering after
    }
}
```

### Opening Screens
```java
MinecraftClient.getInstance().setScreen(new MyScreen());
```

---

## 5. Rendering System (DrawContext)

### Evolution of Rendering
| Version | Rendering Class | Notes |
|---------|----------------|-------|
| Pre-1.20 | `MatrixStack` | Passed directly to render methods |
| 1.20+ | `DrawContext` | Wraps MatrixStack, provides utilities |
| 1.21.6+ | Uses `Matrix3x2fStack` internally | HUD rendering changed significantly |

### DrawContext Key Methods
```java
// Fill a rectangle with color
context.fill(x1, y1, x2, y2, color);

// Draw text
context.drawText(textRenderer, "Hello", x, y, color, shadow);

// Draw texture
context.drawTexture(identifier, x, y, u, v, width, height);

// Gradient fill (vertical)
context.fillGradient(x1, y1, x2, y2, colorTop, colorBottom);

// Access the matrix stack if needed
MatrixStack matrices = context.getMatrices();
```

### Color Format
**CRITICAL (1.21.6+):** Colors are now **ARGB** not RGB!
```java
// Wrong - will be transparent!
int color = 0xFF0000;  // Red with no alpha

// Correct
int color = 0xFFFF0000;  // Full alpha red
```

### Fabric Screen Events
```java
// Before screen renders
ScreenEvents.beforeRender(screen).register((screen, context, mouseX, mouseY, delta) -> {
    // Your code
});

// After screen renders
ScreenEvents.afterRender(screen).register((screen, context, mouseX, mouseY, delta) -> {
    // Your code
});
```

---

## 6. Common Mixin Patterns

### Pattern 1: Injecting Into Parent Class Methods

**Problem:** OptionsScreen doesn't override `render()` - it inherits from Screen.

**Solution:** You CANNOT inject into inherited methods through the child class. Options:

1. **Mixin into the parent class (Screen)** and check instanceof:
```java
@Mixin(Screen.class)
public class ScreenMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if ((Object)this instanceof OptionsScreen) {
            // Custom rendering for OptionsScreen
        }
    }
}
```

2. **Use Fabric Screen Events** (preferred):
```java
ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
    if (screen instanceof OptionsScreen) {
        ScreenEvents.beforeRender(screen).register((s, context, mouseX, mouseY, delta) -> {
            // Custom rendering
        });
    }
});
```

3. **Extend and replace the screen entirely**

### Pattern 2: Cancelling Methods
```java
@Inject(method = "render", at = @At("HEAD"), cancellable = true)
private void cancelRender(CallbackInfo ci) {
    if (shouldCancel) {
        ci.cancel();  // Stops the original method from running
    }
}
```

### Pattern 3: Modifying Return Values
```java
@Inject(method = "shouldPause", at = @At("RETURN"), cancellable = true)
private void modifyPause(CallbackInfoReturnable<Boolean> cir) {
    cir.setReturnValue(false);  // Override the return value
}
```

### Pattern 4: Accessing `this` in Mixin
```java
@Mixin(PlayerEntity.class)
public class PlayerMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity)(Object)this;
        // Now use 'self' as the player instance
    }
}
```

### Pattern 5: Mixin for Interface Implementation
```java
@Mixin(PlayerEntity.class)
public abstract class PlayerMixin implements CustomInterface {
    @Unique
    private int myMod$data;

    @Override
    public int myMod$getData() {
        return myMod$data;
    }
}
```

---

## 7. Troubleshooting Mixin Errors

### Error: "Could not find any targets matching 'methodName'"
**Cause:** Method doesn't exist in target class (might be inherited)

**Solutions:**
1. Check if method is inherited - mixin into parent class instead
2. Verify method signature matches exactly
3. Check mappings (yarn vs intermediary vs mojang)

### Error: "No refMap loaded"
**Cause:** Reference map not generated or not found

**Solutions:**
1. Ensure mixin config specifies refmap: `"refmap": "modid.refmap.json"`
2. Clean and rebuild: `./gradlew clean build`
3. Check that annotation processor is configured

### Error: "Mixin transformation failed"
**Cause:** Various - usually signature mismatch or incompatible injection

**Solutions:**
1. Install **Mixin Conflict Helper** mod for better error messages
2. Check parameter types match exactly
3. Verify injection point exists in target method

### Error: Protected/Private access
**Cause:** Trying to access protected member through cast

**Solution:** Use `MinecraftClient.getInstance()` for global access:
```java
// Wrong
OptionsScreen self = (OptionsScreen)(Object)this;
self.textRenderer;  // Error: protected access

// Correct
TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
```

---

## 8. Multi-Version Support

### Stonecutter Preprocessor
Stonecutter lets you maintain one codebase for multiple Minecraft versions:

```java
//? if MC_VERSION >= 1.21.5 {
context.drawText(textRenderer, text, x, y, color, shadow);
//?} else {
/*context.drawTextWithShadow(textRenderer, text, x, y, color);*/
//?}
```

### Key Version Differences

| Change | Versions | Migration |
|--------|----------|-----------|
| `MatrixStack` → `DrawContext` | 1.20+ | Replace parameter type |
| `DrawContext` internals → `Matrix3x2fStack` | 1.21.6+ | Avoid matrix manipulation |
| `HudRenderCallback` deprecated | 1.21.9+ | Use mixins instead |
| Text color RGB → ARGB | 1.21.6+ | Add alpha to colors |
| `KeyBinding.Category` enum changes | 1.21.9+ | Use new category names |

### Yarn vs Intermediary Mappings
- **Development:** Uses Yarn mappings (human-readable names)
- **Production:** Uses Intermediary (stable across versions)
- **Refmap:** Translates between them automatically

---

## 9. Best Practices

### DO:
- **Prefix unique members** with `modid$` to avoid conflicts
- **Use Fabric events** when available instead of mixins
- **Check instanceof** when mixing into parent classes
- **Test on multiple versions** if supporting them
- **Use `@WrapOperation`** instead of `@Redirect` for compatibility
- **Separate client code** into a `client` package

### DON'T:
- **Don't use `@Overwrite`** unless absolutely necessary
- **Don't use `@Redirect`** if `@WrapOperation` works (conflicts)
- **Don't inject into constructors** at arbitrary points (use TAIL)
- **Don't assume methods exist** in subclasses (check inheritance)
- **Don't hardcode colors** without alpha in 1.21.6+

### Performance Tips
- Avoid creating objects in render methods
- Cache frequently accessed values
- Use `@Unique` fields instead of recalculating
- Profile with Spark or similar tools

---

## 10. Quick Reference Cheat Sheet

### Mixin Annotations
| Annotation | Purpose |
|------------|---------|
| `@Mixin(Class.class)` | Target a class |
| `@Inject` | Add code at injection point |
| `@Shadow` | Reference existing member |
| `@Accessor` | Generate getter/setter |
| `@Invoker` | Generate method caller |
| `@Unique` | Add new member |
| `@Redirect` | Replace instruction (use sparingly) |
| `@ModifyArg` | Change method call argument |
| `@ModifyVariable` | Change local variable |
| `@Final` | Mark shadowed field as final |
| `@Mutable` | Allow modifying final field |

### Injection Points
| Point | Location |
|-------|----------|
| `HEAD` | First instruction |
| `TAIL` | Before final return |
| `RETURN` | Before all returns |
| `INVOKE` | Before method call |
| `INVOKE_ASSIGN` | After method call |
| `FIELD` | At field access |
| `NEW` | Before new object |

### Screen Events (Fabric API)
```java
ScreenEvents.BEFORE_INIT  // Before screen init
ScreenEvents.AFTER_INIT   // After screen init
ScreenEvents.beforeRender(screen)  // Before render
ScreenEvents.afterRender(screen)   // After render
```

### DrawContext Quick Reference
```java
context.fill(x1, y1, x2, y2, color);
context.fillGradient(x1, y1, x2, y2, colorTop, colorBottom);
context.drawText(textRenderer, text, x, y, color, shadow);
context.drawTexture(id, x, y, u, v, width, height);
context.getMatrices();  // Get MatrixStack if needed
```

---

## Sources

1. [Fabric Wiki - Mixin Examples](https://wiki.fabricmc.net/tutorial:mixin_examples)
2. [Fabric Wiki - Mixin Injects](https://fabricmc.net/wiki/tutorial:mixin_injects)
3. [Fabric Documentation - Custom Screens](https://docs.fabricmc.net/develop/rendering/gui/custom-screens)
4. [SpongePowered Mixin GitHub](https://github.com/SpongePowered/Mixin)
5. [SpongePowered Mixin Wiki - Callback Injectors](https://github.com/SpongePowered/Mixin/wiki/Advanced-Mixin-Usage---Callback-Injectors)
6. [SpongePowered Mixin Wiki - Injection Point Reference](https://github.com/SpongePowered/Mixin/wiki/Injection-Point-Reference)
7. [Fabric Wiki - Mixin Accessors](https://wiki.fabricmc.net/tutorial:mixin_accessors)
8. [Fabric Wiki - Mixin Inheritance](https://www.fabricmc.net/wiki/tutorial:mixinheritance)
9. [Fabric Documentation - Events](https://docs.fabricmc.net/develop/events)
10. [Fabric Wiki - Event Index](https://wiki.fabricmc.net/tutorial:event_index)
11. [ScreenEvents API Documentation](https://maven.fabricmc.net/docs/fabric-api-0.89.2+1.20.2/net/fabricmc/fabric/api/client/screen/v1/ScreenEvents.html)
12. [Fabric Documentation - Draw Context](https://docs.fabricmc.net/develop/rendering/draw-context)
13. [DrawContext Yarn API (1.21.8)](https://maven.fabricmc.net/docs/yarn-1.21.8+build.1/net/minecraft/client/gui/DrawContext.html)
14. [Fabric for Minecraft 1.21.6-1.21.8](https://fabricmc.net/2025/06/15/1216.html)
15. [Fabric for Minecraft 1.21.9-1.21.10](https://fabricmc.net/2025/09/23/1219.html)
16. [Fabric Wiki - Mappings](https://wiki.fabricmc.net/tutorial:mappings)
17. [Yarn GitHub Repository](https://github.com/FabricMC/yarn)
18. [Fabric Loom Documentation](https://docs.fabricmc.net/develop/loom/)
19. [Fabric Loom GitHub](https://github.com/FabricMC/fabric-loom)
20. [SpongePowered Mixin Wiki - Obfuscation](https://github.com/SpongePowered/Mixin/wiki/Introduction-to-Mixins---Obfuscation-and-Mixins)
21. [MixinExtras GitHub](https://github.com/LlamaLad7/MixinExtras)
22. [MixinExtras Wiki - WrapOperation](https://github.com/LlamaLad7/MixinExtras/wiki/WrapOperation)
23. [Mixin Conflict Helper Mod](https://modrinth.com/mod/mixin-conflict-helper)
24. [Fabric Wiki - Modding Tips](https://wiki.fabricmc.net/tutorial:modding_tips)
25. [Stonecutter Documentation](https://stonecutter.kikugie.dev/wiki/)
26. [Stonecutter GitHub](https://github.com/SHsuperCM/Stonecutter)
27. [Baeldung - Java ASM Guide](https://www.baeldung.com/java-asm)
28. [NeoForge - Structuring Your Mod](https://docs.neoforged.net/docs/gettingstarted/structuring/)
29. [ServerLifecycleEvents API](https://maven.fabricmc.net/docs/fabric-api-0.110.5+1.21.4/net/fabricmc/fabric/api/event/lifecycle/v1/ServerLifecycleEvents.html)
30. [ClientLifecycleEvents API](https://maven.fabricmc.net/docs/fabric-api-0.85.0+1.20.1/net/fabricmc/fabric/api/client/event/lifecycle/v1/ClientLifecycleEvents.html)
31. [Mixin Basics Tutorial](https://mixin-wiki.readthedocs.io/mixin-basics/)
32. [DeepWiki - Mixin Annotation Processing](https://deepwiki.com/SpongePowered/Mixin/4-annotation-processing)

---

*This guide was compiled to help understand why injecting into `OptionsScreen.render()` fails - the method is inherited from `Screen`, not overridden. The solution is to either mixin into `Screen` class with instanceof checks, use Fabric's ScreenEvents API, or completely replace the screen.*
