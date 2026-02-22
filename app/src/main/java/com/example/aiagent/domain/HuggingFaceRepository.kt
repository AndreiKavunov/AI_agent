package com.example.aiagent.domain

import com.example.aiagent.data.huggingFace.HuggingFaceModel

interface HuggingFaceRepository {
    fun setHuggingFaceModel(modelType: HuggingFaceModel)
    fun getCurrentHuggingFaceModel(): HuggingFaceModel
}