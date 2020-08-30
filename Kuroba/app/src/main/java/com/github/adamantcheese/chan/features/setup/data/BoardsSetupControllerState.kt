package com.github.adamantcheese.chan.features.setup.data

sealed class BoardsSetupControllerState {
  object Loading : BoardsSetupControllerState()
  object Empty : BoardsSetupControllerState()
  data class Error(val errorText: String) : BoardsSetupControllerState()
  data class Data(val boardCellDataList: List<BoardCellData>) : BoardsSetupControllerState()
}