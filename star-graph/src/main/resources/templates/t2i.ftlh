{
"3": {
"inputs": {
"seed": ${config.seed},
"steps": ${config.step},
"cfg": ${config.cfg},
"sampler_name": "${config.samplerName}",
"scheduler": "${config.scheduler}",
"denoise": 1,
"model": [
"4",
0
],
"positive": [
"6",
0
],
"negative": [
"7",
0
],
"latent_image": [
"5",
0
]
},
"class_type": "KSampler",
"_meta": {
"title": "K采样器"
}
},
"4": {
"inputs": {
"ckpt_name": "${config.modelName}"
},
"class_type": "CheckpointLoaderSimple",
"_meta": {
"title": "Checkpoint加载器(简易)"
}
},
"5": {
"inputs": {
"width": ${config.width},
"height": ${config.height},
"batch_size": ${config.size}
},
"class_type": "EmptyLatentImage",
"_meta": {
"title": "空Latent"
}
},
"6": {
"inputs": {
"text": "${config.propmt}",
"clip": [
"4",
1
]
},
"class_type": "CLIPTextEncode",
"_meta": {
"title": "CLIP文本编码器"
}
},
"7": {
"inputs": {
"text": "${config.reverse}",
"clip": [
"4",
1
]
},
"class_type": "CLIPTextEncode",
"_meta": {
"title": "CLIP文本编码器"
}
},
"8": {
"inputs": {
"samples": [
"3",
0
],
"vae": [
"4",
2
]
},
"class_type": "VAEDecode",
"_meta": {
"title": "VAE解码"
}
},
"9": {
"inputs": {
"filename_prefix": "ComfyUI",
"images": [
"8",
0
]
},
"class_type": "SaveImage",
"_meta": {
"title": "保存图像"
}
}
}