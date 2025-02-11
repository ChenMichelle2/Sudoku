package com.example.sudoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                    SudokuGameScreen()

            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SudokuGameScreen() {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var grid by remember { mutableStateOf(createInitialGrid()) }
    var selectedCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var inputValue by remember { mutableStateOf("") }

    fun resetGrid() {
        grid = createInitialGrid()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(title = { Text("Sudoku") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(9),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                items(81) { index ->
                    val row = index / 9
                    val col = index % 9
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .drawBehind {
                                val leftStroke = if (col % 3 == 0) 2.dp.toPx() else 1.dp.toPx()
                                val topStroke = if (row % 3 == 0) 2.dp.toPx() else 1.dp.toPx()
                                val rightStroke = if (col == 8 || (col + 1) % 3 == 0) 2.dp.toPx() else 1.dp.toPx()
                                val bottomStroke = if (row == 8 || (row + 1) % 3 == 0) 2.dp.toPx() else 1.dp.toPx()

                                //top border.
                                drawLine(
                                    color = Color.Black,
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width, 0f),
                                    strokeWidth = topStroke
                                )
                                //left border.
                                drawLine(
                                    color = Color.Black,
                                    start = Offset(0f, 0f),
                                    end = Offset(0f, size.height),
                                    strokeWidth = leftStroke
                                )
                                //right border.
                                drawLine(
                                    color = Color.Black,
                                    start = Offset(size.width, 0f),
                                    end = Offset(size.width, size.height),
                                    strokeWidth = rightStroke
                                )
                                //bottom border.
                                drawLine(
                                    color = Color.Black,
                                    start = Offset(0f, size.height),
                                    end = Offset(size.width, size.height),
                                    strokeWidth = bottomStroke
                                )
                            }
                            .clickable {
                                //only clickable if empty
                                if (grid[row][col] == null) {
                                    selectedCell = row to col
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = grid[row][col]?.toString() ?: "")
                    }
                }
            }

            //reset
            Button(onClick = { resetGrid() }) {
                Text("Reset")
            }
        }

        //input for cell
        if (selectedCell != null) {
            AlertDialog(
                onDismissRequest = {
                    selectedCell = null
                    inputValue = ""
                },
                title = { Text("Enter a number (1-9)") },
                text = {
                    TextField(
                        value = inputValue,
                        onValueChange = { inputValue = it },
                        placeholder = { Text("1-9") }
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val number = inputValue.toIntOrNull()
                            if (number == null || number !in 1..9) {
                                selectedCell = null
                                inputValue = ""
                                return@Button
                            }
                            val (row, col) = selectedCell!!
                            if (isValidMove(grid, row, col, number)) {
                                grid = grid.mapIndexed { r, rowList ->
                                    if (r == row)
                                        rowList.mapIndexed { c, cell ->
                                            if (c == col) number else cell
                                        }
                                    else rowList
                                }
                                if (checkWin(grid)) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("You won!")
                                    }
                                }
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Invalid move")
                                }
                            }
                            selectedCell = null
                            inputValue = ""
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            selectedCell = null
                            inputValue = ""
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

//creates initial grid
fun createInitialGrid(): List<List<Int?>> {
    val firstRow = (1..9).toMutableList().apply { shuffle() }
    val grid = mutableListOf<List<Int?>>()
    grid.add(firstRow.toList())
    repeat(8) { grid.add(List(9) { null }) }
    return grid
}

//checks if move is possible
fun isValidMove(grid: List<List<Int?>>, row: Int, col: Int, number: Int): Boolean {
    if (grid[row].contains(number)) return false
    for (r in 0 until 9) {
        if (grid[r][col] == number) return false
    }
    val blockRowStart = (row / 3) * 3
    val blockColStart = (col / 3) * 3
    for (r in blockRowStart until blockRowStart + 3) {
        for (c in blockColStart until blockColStart + 3) {
            if (grid[r][c] == number) return false
        }
    }
    return true
}

//check if grid filled properly
fun checkWin(grid: List<List<Int?>>): Boolean {
    //check every cell is non-null.
    for (row in grid) {
        if (row.any { it == null }) return false
    }
    //check rows
    for (row in grid) {
        if (row.toSet().size != 9) return false
    }
    //check columns.
    for (col in 0 until 9) {
        val columnNumbers = (0 until 9).map { grid[it][col]!! }
        if (columnNumbers.toSet().size != 9) return false
    }
    //check 3x3
    for (blockRow in 0 until 3) {
        for (blockCol in 0 until 3) {
            val blockNumbers = mutableListOf<Int>()
            for (r in 0 until 3) {
                for (c in 0 until 3) {
                    blockNumbers.add(grid[blockRow * 3 + r][blockCol * 3 + c]!!)
                }
            }
            if (blockNumbers.toSet().size != 9) return false
        }
    }
    return true
}
@Preview(showBackground = true)
@Composable
fun SudokuGameScreenPreview() {
    SudokuGameScreen()
}
