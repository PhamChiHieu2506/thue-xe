/* Dữ liệu hoàn toàn minh họa; không phải hồ sơ hay giao dịch thật. */
window.createRentalDemo = function () {
  const cars = [
    {id:1,licensePlate:'30A-12345',name:'Toyota Vios',brand:'Toyota',seatCount:5,pricePerDay:800000,status:'AVAILABLE'},
    {id:2,licensePlate:'29A-67890',name:'Hyundai Accent',brand:'Hyundai',seatCount:5,pricePerDay:750000,status:'AVAILABLE'},
    {id:3,licensePlate:'30A-99999',name:'Mitsubishi Xpander',brand:'Mitsubishi',seatCount:7,pricePerDay:1000000,status:'RENTED'},
    {id:4,licensePlate:'30G-52816',name:'Mazda CX-5',brand:'Mazda',seatCount:5,pricePerDay:1200000,status:'AVAILABLE'},
    {id:5,licensePlate:'29K-36852',name:'Kia Seltos',brand:'Kia',seatCount:5,pricePerDay:1100000,status:'RENTED'},
    {id:6,licensePlate:'30H-21068',name:'Toyota Corolla Cross',brand:'Toyota',seatCount:5,pricePerDay:1300000,status:'AVAILABLE'},
    {id:7,licensePlate:'30F-85629',name:'Ford Everest',brand:'Ford',seatCount:7,pricePerDay:1700000,status:'MAINTENANCE'},
    {id:8,licensePlate:'29L-47382',name:'Hyundai Grand i10',brand:'Hyundai',seatCount:5,pricePerDay:600000,status:'AVAILABLE'},
    {id:9,licensePlate:'30K-64029',name:'Kia Carnival',brand:'Kia',seatCount:7,pricePerDay:2000000,status:'MAINTENANCE'},
    {id:10,licensePlate:'30L-12683',name:'VinFast VF 8',brand:'VinFast',seatCount:5,pricePerDay:1400000,status:'AVAILABLE'},
    {id:11,licensePlate:'29H-62190',name:'Honda City',brand:'Honda',seatCount:5,pricePerDay:900000,status:'AVAILABLE'},
    {id:12,licensePlate:'30E-31764',name:'Suzuki XL7',brand:'Suzuki',seatCount:7,pricePerDay:1000000,status:'MAINTENANCE'}
  ];
  const names=['Nguyễn Minh Anh','Trần Hoàng Nam','Lê Thu Hà','Phạm Tuấn Kiệt','Đỗ Ngọc Linh','Vũ Đức Huy','Bùi Thanh Tâm','Hoàng Gia Bảo'];
  const customers=names.map((name,i)=>({id:i+1,name,phone:'09000000'+String(i+10),email:'khach'+(i+1)+'@example.com',licenseNumber:'DEMO-GPLX-'+(100+i),licenseStatus:i===6?'PENDING':'VERIFIED'}));
  const today=new Date();today.setHours(0,0,0,0);
  const at=(offset)=>{const d=new Date(today);d.setDate(d.getDate()+offset);return new Date(d.getTime()-d.getTimezoneOffset()*60000).toISOString().slice(0,10)+'T09:00:00'};
  const tuples=[[1,4,1,4,'CHO_DUYET'],[2,6,2,5,'CHO_DUYET'],[3,8,3,6,'CHO_DUYET'],[4,1,1,3,'CHO_COC'],[5,10,4,6,'DA_XAC_NHAN'],[6,3,-1,2,'DANG_THUE'],[8,5,-2,1,'DANG_THUE'],[1,11,-6,-3,'HOAN_THANH'],[2,2,-4,-1,'DA_HUY']];
  const rentals=tuples.map((t,i)=>({id:1001+i,customerId:t[0],carId:t[1],startDate:at(t[2]),endDate:at(t[3]),status:t[4],totalAmount:(t[3]-t[2])*cars.find(c=>c.id===t[1]).pricePerDay,depositAmount:1000000,createdAt:at(-i)}));
  const payments=[];
  for(let m=5;m>=0;m--){for(let k=0;k<4;k++){
    const d=new Date(today.getFullYear(),today.getMonth()-m,Math.min(2+k*3,today.getDate()),10,0,0);
    payments.push({id:2000+payments.length,rentalId:1008,amount:(5-m)*800000+2200000+k*300000,type:'RENT',status:'SUCCESS',paidAt:new Date(d.getTime()-d.getTimezoneOffset()*60000).toISOString().slice(0,19)});
  }}
  payments.push({id:2101,rentalId:1005,amount:1000000,type:'DEPOSIT',status:'SUCCESS',paidAt:at(-1)});
  payments.push({id:2102,rentalId:1004,amount:1000000,type:'DEPOSIT',status:'PENDING',paidAt:at(0)});
  payments.push({id:2103,rentalId:1009,amount:1000000,type:'REFUND',status:'SUCCESS',paidAt:at(-1)});
  payments.push({id:2104,rentalId:1004,amount:1000000,type:'DEPOSIT',status:'FAILED',paidAt:at(-1)});
  return {cars,customers,rentals,payments};
};
