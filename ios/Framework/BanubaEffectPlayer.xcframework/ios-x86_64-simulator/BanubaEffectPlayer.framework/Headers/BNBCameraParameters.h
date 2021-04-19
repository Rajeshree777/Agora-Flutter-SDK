// AUTOGENERATED FILE - DO NOT MODIFY!
// This file generated by Djinni from scene.djinni

#import "BNBProjectionType.h"
#import <Foundation/Foundation.h>

@interface BNBCameraParameters : NSObject
- (nonnull instancetype)initWithProjection:(BNBProjectionType)projection
                                     zNear:(float)zNear
                                      zFar:(float)zFar
                                       fov:(float)fov
                                frameWidth:(int32_t)frameWidth
                               frameHeight:(int32_t)frameHeight;
+ (nonnull instancetype)cameraParametersWithProjection:(BNBProjectionType)projection
                                                 zNear:(float)zNear
                                                  zFar:(float)zFar
                                                   fov:(float)fov
                                            frameWidth:(int32_t)frameWidth
                                           frameHeight:(int32_t)frameHeight;

@property (nonatomic, readonly) BNBProjectionType projection;

@property (nonatomic, readonly) float zNear;

@property (nonatomic, readonly) float zFar;

@property (nonatomic, readonly) float fov;

@property (nonatomic, readonly) int32_t frameWidth;

@property (nonatomic, readonly) int32_t frameHeight;

@end
