#
# SPDX-FileCopyrightText: The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from the proprietary version
$(call inherit-product, vendor/dolby/common/common-vendor.mk)

DOLBY_PATH := vendor/dolby

# Configs
PRODUCT_COPY_FILES += \
    $(DOLBY_PATH)/configs/dolby/dax-default.xml:$(TARGET_COPY_OUT_VENDOR)/etc/dolby/dax-default.xml

# DolbyAtmos
PRODUCT_PACKAGES += \
    DolbyAtmos
    
# Properties
PRODUCT_VENDOR_PROPERTIES += \
    ro.vendor.dolby.dax.version=DAX3_3.7.0.8_r1 \
    vendor.audio.dolby.ds2.enabled=false \
    vendor.audio.dolby.ds2.hardbypass=false

# Sepolicy
BOARD_VENDOR_SEPOLICY_DIRS += \
    $(DOLBY_PATH)/sepolicy/vendor

# VINTF
DEVICE_FRAMEWORK_COMPATIBILITY_MATRIX_FILE += $(DOLBY_PATH)/configs/vintf/dolby_framework_matrix.xml

# Dolby vision Proprietary blobs
PRODUCT_COPY_FILES += \
    $(DOLBY_PATH)/common/proprietary/vendor/etc/dolby_vision.cfg:$(TARGET_COPY_OUT_VENDOR)/etc/dolby_vision.cfg \
    $(DOLBY_PATH)/common/proprietary/vendor/etc/init/vendor.dolbyvision.media.c2@1.0-service.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/vendor.dolbyvision.media.c2@1.0-service.rc

PRODUCT_PACKAGES += \
    c2.dolby.avc.dec \
    c2.dolby.avc.sec.dec \
    c2.dolby.client \
    c2.dolby.egl \
    c2.dolby.hevc.dec \
    c2.dolby.hevc.enc \
    c2.dolby.hevc.sec.dec \
    c2.dolby.store \
    libdolbyottcameracontrol \
    libdolbyvision \
    dolbycodec2

PRODUCT_PACKAGES += \
    libstagefright_foundation-swiitchoff

# Shim
PRODUCT_PACKAGES += \
    libcodec2_hidl_shim.vendor
