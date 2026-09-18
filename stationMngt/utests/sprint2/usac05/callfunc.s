    .equ STDOUT,1
    .equ STDERR,2
    .equ WRITE,64
    .equ EXIT,93
	.equ MAGIC_NUMBER,0x2649843a2649843a

    .section .rodata
msg:
    .ascii "Please respect the correct register usage on the RISC-V processors!\n"


.section .text
    .globl callfunc
        
callfunc:
    addi	sp,sp,-64             # reserve 64 bytes = 12 * 32 bit words	
    sw		ra,60(sp)             # save ra  
    sw		s0,56(sp)             # save s0  = frame pointer 
    sw          s1,52(sp) 	      # registers to save: sp s0 s1 s2 s3 s4 s5 s6  
    sw		s2,48(sp)	      #                    s7 s8 s9 s10 s11 gp tp 
    sw		s3,44(sp)	      #  15 registers * 4 bytes = 60 bytes => 64 bytes (multiple of 16!)	
    sw          s4,40(sp) 					    
    sw          s5,36(sp) 					    
    sw          s6,32(sp) 					    
    sw          s7,28(sp) 					    
    sw          s8,24(sp) 					    
    sw          s9,20(sp) 					    
    sw          s10,16(sp) 					    
    sw          s11,12(sp) 					    
    sw          gp,8(sp) 					    
    sw          tp,4(sp) 	
    addi        s0,sp,64             # s0 = frame pointer = sp + 64 				    

    li          s1,0x1234          # leave "markers" on the registers 
    li          s2,0x1234 
    li          s3,0x1234 
    li          s4,0x1234 
    li          s5,0x1234 
    li          s6,0x1234 
    li          s7,0x1234 
    li          s8,0x1234 
    li          s9,0x1234 
    li          s10,0x1234 
    li          s11,0x1234 
    li          gp,0x1234 
    li          tp,0x1234 
    
    mv          t0,a0 	             # t0 = pointer to function 
    mv		a0,a1                # rearrange function arguments 
    mv          a1,a2 
    mv          a2,a3 
    mv          a3,a4 
    mv          a4,a5 
    mv          a5,a6 
    mv          a6,a7                 # we only support functions with up to 7 arguments 
    
    jalr        ra,t0                 # call *t0 

    li		t0,0x1234            # check if the registers have changed
    bne         t0,s1,reg_error  	
    bne         t0,s2,reg_error  	
    bne         t0,s3,reg_error  	
    bne         t0,s4,reg_error  	
    bne         t0,s5,reg_error  	
    bne         t0,s6,reg_error  	
    bne         t0,s7,reg_error  	
    bne         t0,s8,reg_error  	
    bne         t0,s9,reg_error  	
    bne         t0,s10,reg_error  	
    bne         t0,s11,reg_error  	
    bne         t0,gp,reg_error  	
    bne         t0,tp,reg_error  	

    lw		ra,60(sp)             #  recover saved registers 
    lw		s0,56(sp)             # 
    lw          s1,52(sp) 	      # 
    lw		s2,48(sp)	      #
    lw		s3,44(sp)	      #  
    lw          s4,40(sp) 					    
    lw          s5,36(sp) 					    
    lw          s6,32(sp) 					    
    lw          s7,28(sp) 					    
    lw          s8,24(sp) 					    
    lw          s9,20(sp) 					    
    lw          s10,16(sp) 					    
    lw          s11,12(sp) 					    
    lw          gp,8(sp) 					    
    lw          tp,4(sp) 	

    addi	sp,sp,64 	      # recover stack 
    ret             # retorno (jalr x0, x1, 0)
    
reg_error: 
    lw		ra,60(sp)             #  recover saved registers 
    lw		s0,56(sp)             # 
    lw          s1,52(sp) 	      # 
    lw		s2,48(sp)	      #
    lw		s3,44(sp)	      #  
    lw          s4,40(sp) 					    
    lw          s5,36(sp) 					    
    lw          s6,32(sp) 					    
    lw          s7,28(sp) 					    
    lw          s8,24(sp) 					    
    lw          s9,20(sp) 					    
    lw          s10,16(sp) 					    
    lw          s11,12(sp) 					    
    lw          gp,8(sp) 					    
    lw          tp,4(sp) 	
    addi	sp,sp,64 	      # recover stack 

    li a0, STDERR  # file descriptor
    la a1, msg     # address of string
    li a2, 68       # length of string
    li a7, WRITE   # syscall number for write
    ecall

    li a0, 33       # 0 signals success # 33 was chosen randomly 
    li a7, EXIT
    ecall	

