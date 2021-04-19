// AUTOGENERATED FILE - DO NOT MODIFY!
// This file generated by Djinni from postprocess.djinni

#import <Foundation/Foundation.h>
@class BNBPostprocessStage;
@class BNBPostprocessStageFramebufferApplier;


/** Apply stage on current framebuffer (slower than applying on texture). */
@interface BNBPostprocessStageFramebufferApplier : NSObject

/**
 * @param stage postprocess stage which you need to apply on current framebuffer.
 * @param params parameters for effect.
 * @param width width of the current framebuffer.
 * @param height height of the current framebuffer.
 */
- (void)apply:(nullable BNBPostprocessStage *)stage
       params:(nonnull NSDictionary<NSString *, NSString *> *)params
        width:(int32_t)width
       height:(int32_t)height;

+ (nullable BNBPostprocessStageFramebufferApplier *)create;

@end
